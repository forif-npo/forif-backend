package org.forif_backend.web.staff.dto;

import org.forif_backend.application.user.dto.ReturningMemberRoster;

import java.util.List;

public record ReturningMemberRosterResponse(int actYear, int actSemester, List<Member> members) {
    public record Member(String userId, String userName, String college, String department, String phoneNum) {}

    public static ReturningMemberRosterResponse from(ReturningMemberRoster roster) {
        return new ReturningMemberRosterResponse(roster.actYear(), roster.actSemester(),
                roster.members().stream().map(member -> new Member(
                        member.userId(), member.userName(), member.college(), member.department(), member.phoneNum()))
                        .toList());
    }
}
