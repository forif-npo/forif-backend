package org.forif_backend.domain.study;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.forif_backend.common.exception.ErrorCode;
import org.forif_backend.common.exception.ForifException;

@Getter
@RequiredArgsConstructor
public enum StudyTag {
    DATABASE("database", "데이터베이스"),
    BASIC("basic", "프로그래밍 기초"),
    FRONTEND("frontend", "프론트엔드"),
    BACKEND("backend", "백엔드"),
    FULLSTACK("fullstack", "풀스택"),
    APP("app", "앱"),
    AI("ai", "인공지능"),
    DATA("data", "데이터"),
    SECURITY("security", "보안"),
    GAME("game", "게임"),
    DESIGN("design", "디자인"),
    ALGORITHM("algorithm", "알고리즘"),
    BLOCKCHAIN("blockchain", "블록체인");

    /**
     * 시스템 내부 및 API 통신에서 사용되는 고유 식별 값 (불변)
     */
    private final String value;

    /**
     * 사용자 UI에 표시될 때 사용되는 설명 값 (가변)
     */
    private final String label;

    public static  StudyTag fromValue(String value) {
        return switch (value) {
            case "database" -> DATABASE;
            case "basic" -> BASIC;
            case "frontend" -> FRONTEND;
            case "backend" -> BACKEND;
            case "fullstack" -> FULLSTACK;
            case "app" -> APP;
            case "ai" -> AI;
            case "data" -> DATA;
            case "security" -> SECURITY;
            case "game" -> GAME;
            case "design" -> DESIGN;
            case "algorithm" -> ALGORITHM;
            case "blockchain" -> BLOCKCHAIN;
            default -> throw new ForifException(ErrorCode.INTERNAL_SERVER_ERROR, "해당하는 StudyTag가 없습니다. value: " + value);
        };
    }
}