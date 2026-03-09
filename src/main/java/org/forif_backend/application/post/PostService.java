package org.forif_backend.application.post;

import lombok.RequiredArgsConstructor;
import org.forif_backend.application.file.dto.FileInfo;
import org.forif_backend.application.file.port.out.FilePort;
import org.forif_backend.application.post.dto.PostDto;
import org.forif_backend.application.post.dto.PostListResult;
import org.forif_backend.common.exception.ErrorCode;
import org.forif_backend.common.exception.ForifException;
import org.forif_backend.domain.post.Post;
import org.forif_backend.domain.post.PostFile;
import org.forif_backend.domain.post.PostFileRepository;
import org.forif_backend.domain.post.PostRepository;
import org.forif_backend.domain.staff.StaffAccount;
import org.forif_backend.domain.staff.StaffAccountRepository;
import org.forif_backend.domain.staff.StaffRole;
import org.forif_backend.domain.user.User;
import org.forif_backend.domain.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostService {

    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final StaffAccountRepository staffAccountRepository;
    private final PostFileRepository postFileRepository;
    private final FilePort filePort;

    private static final String POST_TYPE_FAQ = "FAQ";
    private static final String POST_TYPE_ANNOUNCEMENT = "공지사항";
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB

    public PostListResult getFAQs(Long page, Long pageSize, String search) {
        return getPostsByType(POST_TYPE_FAQ, search, page, pageSize);
    }

    public PostListResult getAnnouncements(Long page, Long pageSize, String search) {
        return getPostsByType(POST_TYPE_ANNOUNCEMENT, search, page, pageSize);
    }

    public PostDto getAnnouncement(Integer id) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new ForifException(ErrorCode.SPECIFIC_NOTICE_NOT_FOUND));

        if (!POST_TYPE_ANNOUNCEMENT.equals(post.getPostType())) {
            throw new ForifException(ErrorCode.SPECIFIC_NOTICE_NOT_FOUND);
        }

        return convertToDto(post);
    }

    @Transactional
    public void createAnnouncement(Long userId, String title, String content, String tag, MultipartFile[] images) {
        verifyAdminRole(userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ForifException(ErrorCode.USER_NOT_FOUND));

        Post post = Post.createPost(user, POST_TYPE_ANNOUNCEMENT, title, content, tag);
        postRepository.save(post);

        // 이미지 업로드 및 PostFile에 저장
        if (images != null && images.length > 0) {
            uploadAndSaveImages(post, images);
        }
    }

    @Transactional
    public void updateAnnouncement(Long userId, Integer postId, String title, String content, String tag, MultipartFile[] images) {
        verifyAdminRole(userId);

        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ForifException(ErrorCode.SPECIFIC_NOTICE_NOT_FOUND));

        if (!POST_TYPE_ANNOUNCEMENT.equals(post.getPostType())) {
            throw new ForifException(ErrorCode.SPECIFIC_NOTICE_NOT_FOUND);
        }

        post.update(title, content, tag);

        // 기존 이미지를 S3에서 삭제
        deletePostFiles(postId);

        // 새로운 이미지 업로드 및 저장
        if (images != null && images.length > 0) {
            uploadAndSaveImages(post, images);
        }
    }

    @Transactional
    public void deleteAnnouncement(Long userId, Integer postId) {
        verifyAdminRole(userId);

        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ForifException(ErrorCode.SPECIFIC_NOTICE_NOT_FOUND));

        if (!POST_TYPE_ANNOUNCEMENT.equals(post.getPostType())) {
            throw new ForifException(ErrorCode.SPECIFIC_NOTICE_NOT_FOUND);
        }

        // S3와 DB에서 파일 삭제
        deletePostFiles(postId);

        // 게시글 삭제
        postRepository.deleteById(postId);
    }

    @Transactional
    public void createFAQ(Long userId, String title, String content, String tag) {
        verifyAdminRole(userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ForifException(ErrorCode.USER_NOT_FOUND));

        Post post = Post.createPost(user, POST_TYPE_FAQ, title, content, tag);
        postRepository.save(post);
    }

    @Transactional
    public void updateFAQ(Long userId, Integer postId, String title, String content, String tag) {
        verifyAdminRole(userId);

        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ForifException(ErrorCode.SPECIFIC_FAQ_NOT_FOUND));

        if (!POST_TYPE_FAQ.equals(post.getPostType())) {
            throw new ForifException(ErrorCode.SPECIFIC_FAQ_NOT_FOUND);
        }

        post.update(title, content, tag);
    }

    @Transactional
    public void deleteFAQ(Long userId, Integer postId) {
        verifyAdminRole(userId);

        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ForifException(ErrorCode.SPECIFIC_FAQ_NOT_FOUND));

        if (!POST_TYPE_FAQ.equals(post.getPostType())) {
            throw new ForifException(ErrorCode.SPECIFIC_FAQ_NOT_FOUND);
        }

        postRepository.deleteById(postId);
    }

    private PostListResult getPostsByType(String postType, String search, Long page, Long pageSize) {
        // 타입 검증
        validatePostType(postType);

        Long offset = page * pageSize;
        List<Post> posts = postRepository.findByPostType(postType, search, offset, pageSize);

        List<PostDto> postDtos = posts.stream()
                .map(this::convertToDto)
                .toList();


        return PostListResult.builder()
                .posts(postDtos)
                .build();
    }

    private void validatePostType(String postType) {
        if (!POST_TYPE_FAQ.equals(postType) && !POST_TYPE_ANNOUNCEMENT.equals(postType)) {
            throw new ForifException(ErrorCode.INVALID_INPUT);
        }
    }

    private void validateImageFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new ForifException(ErrorCode.INVALID_FILE_ATTACHMENT);
        }

        // 파일 크기 검증 (5MB)
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new ForifException(ErrorCode.INVALID_FILE_ATTACHMENT);
        }

        // 이미지 파일 타입 검증
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new ForifException(ErrorCode.INVALID_FILE_ATTACHMENT);
        }
    }

    private PostDto convertToDto(Post post) {
        List<PostFile> postFiles = postFileRepository.findByPostIdOrderByFileNum(post.getId());
        List<String> imageUrls = postFiles.stream()
                .map(postFile -> filePort.generatePresignedViewUrl(postFile.getFileUrl()).presignedUrl())
                .toList();

        return PostDto.builder()
                .postId(post.getId())
                .authorId(post.getUser().getId())
                .authorName(post.getUser().getUserName())
                .type(post.getPostType())
                .title(post.getTitle())
                .content(post.getContent())
                .tag(post.getTag())
                .createdAt(post.getCreatedAt())
                .imageUrls(imageUrls)
                .build();
    }

    private void uploadAndSaveImages(Post post, MultipartFile[] images) {
        for (int i = 0; i < images.length; i++) {
            MultipartFile image = images[i];

            // 파일 검증
            validateImageFile(image);

            // S3에 실제로 업로드하고 objectKey 받기
            String objectKey = filePort.uploadFile(image);

            // 파일 타입 추출
            String fileType = extractFileType(image.getOriginalFilename());

            // PostFile 저장 (objectKey를 fileUrl에 저장)
            PostFile postFile = PostFile.createPostFile(post, i + 1, fileType, objectKey);
            postFileRepository.save(postFile);
        }
    }

    private String extractFileType(String filename) {
        if (filename == null) {
            return "image";
        }
        int lastDotIndex = filename.lastIndexOf('.');
        if (lastDotIndex != -1 && lastDotIndex < filename.length() - 1) {
            return filename.substring(lastDotIndex + 1);
        }
        return "image";
    }

    private void deletePostFiles(Integer postId) {
        List<PostFile> postFiles = postFileRepository.findByPostId(postId);

        // S3에서 파일 삭제
        for (PostFile postFile : postFiles) {
            try {
                filePort.deleteFile(postFile.getFileUrl());
            } catch (Exception e) {
                // 삭제 실패 시 로그만 남기고 계속 진행 (이미 삭제된 파일일 수 있음)
            }
        }

        // DB에서 PostFile 삭제
        postFileRepository.deleteByPostId(postId);
    }

    private void verifyAdminRole(Long userId) {
        StaffAccount staffAccount = staffAccountRepository.findByUserId(userId)
                .orElseThrow(() -> new ForifException(ErrorCode.INSUFFICIENT_PERMISSION));

        if (staffAccount.getRole() != StaffRole.ADMIN) {
            throw new ForifException(ErrorCode.INSUFFICIENT_PERMISSION);
        }
    }
}
