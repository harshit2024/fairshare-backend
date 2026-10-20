
package com.fairShare.fairshare_backend.groups;

import com.fairShare.fairshare_backend.groups.dto.*;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/groups")
public class GroupController {

    private final GroupService groupService;

    public GroupController(GroupService groupService) {
        this.groupService = groupService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public GroupResponse create(
            @Valid @RequestBody CreateGroupRequest request) {
        return groupService.create(request);
    }

    @GetMapping
    public List<GroupResponse> getMyGroups() {
        return groupService.getMyGroups();
    }

    @GetMapping("/{groupId}")
    public GroupResponse getById(
            @PathVariable UUID groupId) {
        return groupService.getById(groupId);
    }

    @PostMapping("/{groupId}/members")
    @ResponseStatus(HttpStatus.CREATED)
    public void addMember(
            @PathVariable UUID groupId,
            @Valid @RequestBody AddMemberRequest request) {
        groupService.addMember(groupId, request);
    }

    @GetMapping("/{groupId}/members")
    public List<GroupMember> getMembers(
            @PathVariable UUID groupId) {
        return groupService.getMembers(groupId);
    }

    @DeleteMapping("/{groupId}/members/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeMember(
            @PathVariable UUID groupId,
            @PathVariable UUID userId) {
        groupService.removeMember(groupId, userId);
    }
}