package org.forif_backend.application.study;

import lombok.extern.slf4j.Slf4j;
import org.forif_backend.common.exception.ErrorCode;
import org.forif_backend.common.exception.ForifException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * 수료증 이미지 생성기.
 * 레거시 파이썬 스크립트(FORIF_certificate)의 Pillow 렌더링을 Java Graphics2D로 포팅했다.
 * 좌표는 레거시 config.yaml과 동일하며, Pillow는 텍스트 좌상단 기준이므로
 * drawString(베이스라인 기준) 호출 시 ascent를 더해 보정한다.
 */
@Slf4j
@Component
public class CertificateImageGenerator {

    // 레거시 config.yaml의 text_positions와 동일
    private static final int IMAGE_WIDTH = 1280;
    private static final int FONT_SMALL = 21;
    private static final int FONT_LARGE = 48;
    private static final int[] POS_DURATIONS = {300, 282};
    private static final int[] POS_DEPARTMENT = {652, 282};
    private static final int[] POS_STUDENT_NUMBER = {940, 282};
    private static final int POS_STUDENT_NAME_Y = 360;
    private static final int POS_STUDY_NAME_Y = 500;
    private static final int[] POS_DATE = {575, 640};
    // "President, FORIF" 라벨 아래 회장 이름 자리 (레거시에서는 템플릿에 수기로 넣던 부분)
    private static final int FONT_PRESIDENT = 26;
    private static final int[] POS_PRESIDENT_NAME = {1008, 632};
    // 회장 이름 오른쪽 서명 합성 영역 {x, y, 최대폭, 최대높이} — 비율 유지 축소 후 세로 중앙 정렬
    private static final int[] SIGNATURE_BOX = {1095, 615, 115, 52};
    // 이 값 이상으로 밝은 픽셀(흰 종이 배경 등)은 투명 처리해 이름/라벨을 가리지 않게 한다
    private static final int WHITE_TRANSPARENCY_THRESHOLD = 235;

    private final ResourceLoader resourceLoader;
    private final String templatePath;
    private final String fontPath;

    public CertificateImageGenerator(
            ResourceLoader resourceLoader,
            @Value("${app.certificate.template-path:classpath:certificate/certification_base.png}") String templatePath,
            @Value("${app.certificate.font-path:classpath:certificate/GmarketSansTTFMedium.ttf}") String fontPath
    ) {
        this.resourceLoader = resourceLoader;
        this.templatePath = templatePath;
        this.fontPath = fontPath;
    }

    /**
     * 수료증 PNG 생성
     *
     * @param studentName    이름
     * @param studentNumber  학번
     * @param departmentName 학과
     * @param studyName      스터디명
     * @param activityPeriod 활동 기간 (예: 2026.03.02.~2026.06.20.)
     * @param issueDate      발급일 (예: 2026. 07. 07.)
     * @param presidentName  발급 시점의 회장 이름 (우하단 President, FORIF 아래 표기)
     * @param signatureImage 회장 서명 이미지 (null이면 서명 없이 생성)
     */
    public byte[] generate(String studentName, String studentNumber, String departmentName,
                           String studyName, String activityPeriod, String issueDate,
                           String presidentName, byte[] signatureImage) {
        // 레거시 데이터에 섞인 소프트 하이픈(U+00AD) 등 보이지 않는 문자가 수료증에 찍히지 않도록 제거
        studentName = sanitize(studentName);
        departmentName = sanitize(departmentName);
        studyName = sanitize(studyName);
        presidentName = sanitize(presidentName);
        try {
            BufferedImage image = loadTemplate();
            Font baseFont = loadBaseFont();
            Font smallFont = baseFont.deriveFont(Font.PLAIN, FONT_SMALL);
            Font largeFont = baseFont.deriveFont(Font.PLAIN, FONT_LARGE);
            Font presidentFont = baseFont.deriveFont(Font.PLAIN, FONT_PRESIDENT);

            Graphics2D graphics = image.createGraphics();
            graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                    RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            graphics.setColor(Color.BLACK);

            drawTopLeft(graphics, smallFont, activityPeriod, POS_DURATIONS[0], POS_DURATIONS[1]);
            drawTopLeft(graphics, smallFont, departmentName, POS_DEPARTMENT[0], POS_DEPARTMENT[1]);
            drawTopLeft(graphics, smallFont, studentNumber, POS_STUDENT_NUMBER[0], POS_STUDENT_NUMBER[1]);
            drawCentered(graphics, largeFont, studentName, POS_STUDENT_NAME_Y);
            drawCentered(graphics, largeFont, studyName, POS_STUDY_NAME_Y);
            drawTopLeft(graphics, smallFont, issueDate, POS_DATE[0], POS_DATE[1]);
            if (!presidentName.isBlank()) {
                drawTopLeft(graphics, presidentFont, presidentName,
                        POS_PRESIDENT_NAME[0], POS_PRESIDENT_NAME[1]);
            }
            if (signatureImage != null && signatureImage.length > 0) {
                drawSignature(graphics, signatureImage);
            }

            graphics.dispose();

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            ImageIO.write(image, "png", outputStream);
            return outputStream.toByteArray();
        } catch (IOException e) {
            log.error("수료증 이미지 생성 실패: {}({})", studentName, studentNumber, e);
            throw new ForifException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    private String sanitize(String text) {
        if (text == null) {
            return "";
        }
        return text.replaceAll("[\\u00AD\\u200B\\u200C\\u200D\\uFEFF]", "").trim();
    }

    private void drawTopLeft(Graphics2D graphics, Font font, String text, int x, int y) {
        graphics.setFont(font);
        FontMetrics metrics = graphics.getFontMetrics(font);
        graphics.drawString(text, x, y + metrics.getAscent());
    }

    private void drawSignature(Graphics2D graphics, byte[] signatureImage) throws IOException {
        BufferedImage signature = ImageIO.read(new java.io.ByteArrayInputStream(signatureImage));
        if (signature == null) {
            throw new IOException("서명 이미지를 읽을 수 없습니다.");
        }
        signature = withWhiteBackgroundRemoved(signature);
        double scale = Math.min(
                (double) SIGNATURE_BOX[2] / signature.getWidth(),
                (double) SIGNATURE_BOX[3] / signature.getHeight());
        int width = Math.max(1, (int) (signature.getWidth() * scale));
        int height = Math.max(1, (int) (signature.getHeight() * scale));
        int x = SIGNATURE_BOX[0];
        int y = SIGNATURE_BOX[1] + (SIGNATURE_BOX[3] - height) / 2;

        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        graphics.drawImage(signature, x, y, width, height, null);
    }

    /**
     * 흰 종이에 스캔한 서명도 쓸 수 있도록 밝은 배경 픽셀을 투명 처리한다.
     * (투명 배경 PNG는 그대로 유지된다)
     */
    private BufferedImage withWhiteBackgroundRemoved(BufferedImage source) {
        BufferedImage result = new BufferedImage(
                source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < source.getHeight(); y++) {
            for (int x = 0; x < source.getWidth(); x++) {
                int argb = source.getRGB(x, y);
                int red = (argb >> 16) & 0xFF;
                int green = (argb >> 8) & 0xFF;
                int blue = argb & 0xFF;
                boolean nearWhite = red >= WHITE_TRANSPARENCY_THRESHOLD
                        && green >= WHITE_TRANSPARENCY_THRESHOLD
                        && blue >= WHITE_TRANSPARENCY_THRESHOLD;
                result.setRGB(x, y, nearWhite ? 0x00000000 : argb);
            }
        }
        return result;
    }

    private void drawCentered(Graphics2D graphics, Font font, String text, int y) {
        graphics.setFont(font);
        FontMetrics metrics = graphics.getFontMetrics(font);
        int x = (IMAGE_WIDTH - metrics.stringWidth(text)) / 2;
        graphics.drawString(text, x, y + metrics.getAscent());
    }

    private BufferedImage loadTemplate() throws IOException {
        Resource resource = resourceLoader.getResource(templatePath);
        try (InputStream inputStream = resource.getInputStream()) {
            BufferedImage image = ImageIO.read(inputStream);
            if (image == null) {
                throw new IOException("수료증 템플릿 이미지를 읽을 수 없습니다: " + templatePath);
            }
            return image;
        }
    }

    private Font loadBaseFont() throws IOException {
        Resource resource = resourceLoader.getResource(fontPath);
        try (InputStream inputStream = resource.getInputStream()) {
            return Font.createFont(Font.TRUETYPE_FONT, inputStream);
        } catch (java.awt.FontFormatException e) {
            throw new IOException("수료증 폰트를 읽을 수 없습니다: " + fontPath, e);
        }
    }
}
