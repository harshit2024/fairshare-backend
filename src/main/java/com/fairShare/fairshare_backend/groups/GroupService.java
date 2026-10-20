
package com.fairShare.fairshare_backend.groups;

import com.fairShare.fairshare_backend.groups.dto.*;
import com.fairShare.fairshare_backend.security.CurrentUserService;
import com.fairShare.fairshare_backend.users.UserRepository;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
public class GroupService {

    private final GroupRepository groupRepository;
    private final GroupMemberRepository memberRepository;
    private final UserRepository userRepository;
    private final CurrentUserService currentUserService;

    public GroupService(
            GroupRepository groupRepository,
            GroupMemberRepository memberRepository,
            UserRepository userRepository,
            CurrentUserService currentUserService) {
        this.groupRepository = groupRepository;
        this.memberRepository = memberRepository;
        this.userRepository = userRepository;
        this.currentUserService = currentUserService;
    }

    @Transactional
    public GroupResponse create(CreateGroupRequest request) {
        UUID userId = currentUserService.getCurrentUserId();

        Group group = new Group();
        group.setName(request.name().trim());
        group.setDescription(request.description());
        group.setDefaultCurrency(
                request.defaultCurrency() == null ||
                        request.defaultCurrency().isBlank()
                        ? "INR"
                        : request.defaultCurrency().toUpperCase()
        );
        group.setCreatedBy(userId);
        group.setOwnerId(userId);

        Group saved = groupRepository.save(group);

        GroupMember owner = new GroupMember();
        owner.setGroupId(saved.getId());
        owner.setUserId(userId);
        owner.setRole("ADMIN");

        memberRepository.save(owner);

        return GroupResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public List<GroupResponse> getMyGroups() {
        UUID userId = currentUserService.getCurrentUserId();

        return memberRepository.findByUserId(userId)
                .stream()
                .map(GroupMember::getGroupId)
                .distinct()
                .map(id -> groupRepository.findById(id)
                        .orElseThrow(() -> new ResponseStatusException(
                                HttpStatus.NOT_FOUND, "Group not found")))
                .map(GroupResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public GroupResponse getById(UUID groupId) {
        requireMember(groupId);

        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Group not found"));

        return GroupResponse.from(group);
    }

    @Transactional
    public void addMember(
            UUID groupId, AddMemberRequest request) {
        UUID actorId = currentUserService.getCurrentUserId();
        requireAdmin(groupId, actorId);

        UUID newUserId = request.userId();

        if (!userRepository.existsById(newUserId)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "User not found");
        }

        if (memberRepository.existsByGroupIdAndUserId(
                groupId, newUserId)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "User is already a member");
        }

        GroupMember member = new GroupMember();
        member.setGroupId(groupId);
        member.setUserId(newUserId);
        member.setRole("MEMBER");

        memberRepository.save(member);
    }

    @Transactional(readOnly = true)
    public List<GroupMember> getMembers(UUID groupId) {
        requireMember(groupId);
        return memberRepository.findByGroupId(groupId);
    }

    @Transactional
    public void removeMember(UUID groupId, UUID userId) {
        UUID actorId = currentUserService.getCurrentUserId();
        requireAdmin(groupId, actorId);

        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Group not found"));

        if (group.getCreatedBy().equals(userId)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "The group owner cannot be removed");
        }

        if (!memberRepository.existsByGroupIdAndUserId(
                groupId, userId)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Member not found");
        }

        memberRepository.deleteByGroupIdAndUserId(
                groupId, userId);
    }

    private void requireMember(UUID groupId) {
        UUID userId = currentUserService.getCurrentUserId();

        if (!memberRepository.existsByGroupIdAndUserId(
                groupId, userId)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "You are not a member of this group");
        }
    }

    private void requireAdmin(UUID groupId, UUID userId) {
        GroupMember member = memberRepository
                .findByGroupIdAndUserId(groupId, userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "You are not a member of this group"));

        if (!"ADMIN".equals(member.getRole())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Group admin permission required");
        }
    }
}