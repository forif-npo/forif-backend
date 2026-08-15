package org.forif_backend.application.study;

import lombok.RequiredArgsConstructor;
import org.forif_backend.application.study.dto.AttendanceCommand;
import org.forif_backend.application.study.dto.StudyAttendanceResult;
import org.forif_backend.common.exception.ErrorCode;
import org.forif_backend.common.exception.ForifException;
import org.forif_backend.domain.study.AttendanceStatus;
import org.forif_backend.domain.study.Study;
import org.forif_backend.domain.study.StudyAttendance;
import org.forif_backend.domain.study.StudyAttendanceRepository;
import org.forif_backend.domain.study.StudyRepository;
import org.forif_backend.domain.study.StudyUser;
import org.forif_backend.domain.study.StudyUserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StudyAttendanceService {

    private final StudyRepository studyRepository;
    private final StudyMentorAccess studyMentorAccess;
    private final StudyUserRepository studyUserRepository;
    private final StudyAttendanceRepository studyAttendanceRepository;

    /**
     * 스터디 출석 현황 조회 (멘토 전용)
     * 멘티 전원과 주차별 출석 기록을 반환한다.
     */
    @Transactional(readOnly = true)
    public StudyAttendanceResult getAttendance(Long mentorId, Integer studyId) {
        Study study = getStudyIfMentor(mentorId, studyId);

        List<StudyUser> mentees = studyUserRepository.findAllByStudyId(studyId);

        // userId → 주차별 출석 기록
        Map<Long, List<StudyAttendance>> attendanceByUser = studyAttendanceRepository.findAllByStudyId(studyId)
                .stream()
                .collect(Collectors.groupingBy(sa -> sa.getUser().getId()));

        List<StudyAttendanceResult.MenteeAttendance> menteeResults = mentees.stream()
                .map(mentee -> StudyAttendanceResult.MenteeAttendance.builder()
                        .userId(mentee.getUser().getId())
                        .userName(mentee.getUser().getUserName())
                        .department(mentee.getUser().getDepartment())
                        .records(attendanceByUser.getOrDefault(mentee.getUser().getId(), List.of())
                                .stream()
                                .sorted((a, b) -> Integer.compare(a.getWeekNum(), b.getWeekNum()))
                                .map(sa -> StudyAttendanceResult.AttendanceRecord.builder()
                                        .weekNum(sa.getWeekNum())
                                        .status(sa.getAttendanceStatus() != null
                                                ? sa.getAttendanceStatus().getValue()
                                                : null)
                                        .studyDate(sa.getStudyDate())
                                        .build())
                                .toList())
                        .build())
                .toList();

        return StudyAttendanceResult.builder()
                .studyId(study.getId())
                .studyName(study.getStudyName())
                .mentees(menteeResults)
                .build();
    }

    /**
     * 출석 기록 일괄 upsert (멘토 전용)
     * 같은 (유저, 주차) 기록이 있으면 상태를 갱신하고 없으면 새로 생성한다.
     */
    @Transactional
    public void updateAttendance(Long mentorId, Integer studyId, List<AttendanceCommand> commands) {
        Study study = getStudyIfActiveMentor(mentorId, studyId);

        Map<Long, StudyUser> menteeMap = studyUserRepository.findAllByStudyId(studyId).stream()
                .collect(Collectors.toMap(su -> su.getUser().getId(), su -> su));

        // (userId:weekNum) → 기존 기록
        Map<String, StudyAttendance> existing = studyAttendanceRepository.findAllByStudyId(studyId).stream()
                .collect(Collectors.toMap(
                        sa -> sa.getUser().getId() + ":" + sa.getWeekNum(),
                        sa -> sa
                ));

        List<StudyAttendance> toSave = new ArrayList<>();
        for (AttendanceCommand command : commands) {
            StudyUser mentee = menteeMap.get(command.userId());
            if (mentee == null) {
                throw new ForifException(ErrorCode.USER_NOT_FOUND);
            }
            if (command.weekNum() < 1) {
                throw new ForifException(ErrorCode.INVALID_INPUT);
            }

            AttendanceStatus status = AttendanceStatus.fromValue(command.status());

            StudyAttendance record = existing.get(command.userId() + ":" + command.weekNum());
            if (record != null) {
                record.updateStatus(status, command.studyDate());
            } else {
                toSave.add(StudyAttendance.create(
                        study, mentee.getUser(), command.weekNum(), status, command.studyDate()));
            }
        }

        if (!toSave.isEmpty()) {
            studyAttendanceRepository.saveAll(toSave);
        }
    }

    /** 조회용. 지난 학기 출석부도 본인이 멘토였으면 볼 수 있다. */
    private Study getStudyIfMentor(Long userId, Integer studyId) {
        Study study = studyRepository.findStudyById(studyId)
                .orElseThrow(() -> new ForifException(ErrorCode.STUDY_NOT_FOUND));

        studyMentorAccess.requireMentor(study, userId);
        requireAttendanceTarget(study);
        return study;
    }

    /** 변경용. 활동 학기 스터디만 출석을 기록할 수 있다. */
    private Study getStudyIfActiveMentor(Long userId, Integer studyId) {
        Study study = studyRepository.findStudyById(studyId)
                .orElseThrow(() -> new ForifException(ErrorCode.STUDY_NOT_FOUND));

        studyMentorAccess.requireMentorOfActiveSemester(study, userId);
        requireAttendanceTarget(study);
        return study;
    }

    private void requireAttendanceTarget(Study study) {
        if (study.isAutonomousStudy()) {
            throw new ForifException(ErrorCode.AUTONOMOUS_STUDY_OPERATION_NOT_ALLOWED);
        }
    }
}
