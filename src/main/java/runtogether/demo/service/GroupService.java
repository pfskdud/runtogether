package runtogether.demo.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import runtogether.demo.domain.*;
import runtogether.demo.dto.GroupDto;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GroupService {

    private final RunningGroupRepository groupRepository;
    private final UserGroupRepository userGroupRepository;
    private final UserRepository userRepository;
    private final CourseRepository courseRepository;

    // 1. 그룹 생성
    @Transactional
    public Long createGroup(String email, GroupDto.CreateRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("유저 없음"));

        RunningGroup group = new RunningGroup(
                request.getGroupName(),
                request.getStartDate(),
                request.getEndDate(),
                user
        );

        RunningGroup savedGroup = groupRepository.save(group);
        userGroupRepository.save(new UserGroup(user, savedGroup));

        return savedGroup.getId();
    }

    // 2. 코스 추가
    @Transactional
    public void addCourse(Long groupId, GroupDto.AddCourseRequest request) {
        RunningGroup group = groupRepository.findById(groupId)
                .orElseThrow(() -> new IllegalArgumentException("그룹 없음"));

        Course course = new Course(
                request.getTitle(),
                request.getDistance(),
                request.getDescription(),
                group
        );
        courseRepository.save(course);
    }

    // 3. 그룹 참여
    @Transactional
    public String joinGroup(String email, Long groupId) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("유저 없음"));
        RunningGroup group = groupRepository.findById(groupId)
                .orElseThrow(() -> new IllegalArgumentException("그룹 없음"));

        if (userGroupRepository.existsByUserAndRunningGroup(user, group)) {
            throw new IllegalArgumentException("이미 가입된 그룹입니다.");
        }

        userGroupRepository.save(new UserGroup(user, group));
        return "그룹 가입 완료!";
    }

    // ★ [추가됨] 4. 전체 그룹 목록 조회 (이것도 있어야 컨트롤러가 에러 안 남!)
    @Transactional(readOnly = true)
    public List<GroupDto.Response> getAllGroups() {
        return groupRepository.findAll().stream()
                .map(group -> new GroupDto.Response(
                        group.getId(),
                        group.getName(),
                        group.getStartDate().toString(),
                        group.getEndDate().toString(),
                        group.getOwner().getNickname()
                ))
                .collect(Collectors.toList());
    }
}