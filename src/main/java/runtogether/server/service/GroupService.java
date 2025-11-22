package runtogether.server.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import runtogether.server.domain.*;
import runtogether.server.dto.GroupDto;

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
                request.getDescription(),
                request.isSecret(),
                user
        );

        RunningGroup savedGroup = groupRepository.save(group);
        userGroupRepository.save(new UserGroup(user, savedGroup));

        return savedGroup.getId();
    }

    // ★ [추가됨] 컨트롤러에서 그룹 정보를 확인하기 위해 필요한 헬퍼 메서드
    @Transactional(readOnly = true)
    public RunningGroup getGroup(Long groupId) {
        return groupRepository.findById(groupId)
                .orElseThrow(() -> new IllegalArgumentException("그룹 없음"));
    }

    // 2. 코스 추가
    @Transactional
    public void addCourse(Long groupId, GroupDto.AddCourseRequest request) {
        RunningGroup group = groupRepository.findById(groupId)
                .orElseThrow(() -> new IllegalArgumentException("그룹 없음"));

        Course course = new Course(
                request.getTitle(),
                request.getDistance(),
                request.getExpectedTime(),
                request.getPathData(),
                request.getDescription(),
                group
        );
        courseRepository.save(course);
    }

    // 3. 그룹 참여 (★ 여기가 수정되었습니다!)
    // 매개변수에 String inputCode가 추가됨
    @Transactional
    public String joinGroup(String email, Long groupId, String inputCode) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("유저 없음"));
        RunningGroup group = groupRepository.findById(groupId)
                .orElseThrow(() -> new IllegalArgumentException("그룹 없음"));

        // 이미 가입했는지 확인
        if (userGroupRepository.existsByUserAndRunningGroup(user, group)) {
            throw new IllegalArgumentException("이미 가입된 그룹입니다.");
        }

        // ★ 비공개 그룹인데 코드가 틀렸는지 확인
        if (group.isSecret()) {
            if (inputCode == null || !inputCode.equals(group.getAccessCode())) {
                throw new IllegalArgumentException("입장 코드가 올바르지 않습니다.");
            }
        }

        userGroupRepository.save(new UserGroup(user, group));
        return "그룹 가입 완료!";
    }

    // 4. 전체 그룹 목록 조회
    @Transactional(readOnly = true)
    public List<GroupDto.Response> getAllGroups() {
        return groupRepository.findAll().stream()
                .map(group -> new GroupDto.Response(
                        group.getId(),
                        group.getName(),
                        group.getDescription(),
                        group.isSecret(),
                        group.getStartDate().toString(),
                        group.getEndDate().toString(),
                        group.getOwner().getNickname()
                ))
                .collect(Collectors.toList());
    }
}