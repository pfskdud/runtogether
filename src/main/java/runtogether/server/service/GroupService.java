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
                request.getDescription(),
                request.isSecret(),
                request.isSearchable(), // ★ 추가
                request.getMaxPeople(), // ★ 추가
                request.getTags(),      // ★ 추가
                user
        );

        RunningGroup savedGroup = groupRepository.save(group);
        userGroupRepository.save(new UserGroup(user, savedGroup));

        return savedGroup.getId();
    }

    // 컨트롤러에서 그룹 정보를 확인하기 위해 필요한 헬퍼 메서드
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

    // 4. 그룹 목록 조회 (검색 + 필터링 통합)
    @Transactional(readOnly = true)
    public List<GroupDto.Response> getFilteredGroups(String keyword, String status, String type) {
        List<RunningGroup> groups;

        if (keyword != null && !keyword.trim().isEmpty()) {
            groups = groupRepository.findAllByNameContainingOrTagsContaining(keyword, keyword);
        } else {
            groups = groupRepository.findAll();
        }

        return groups.stream()
                .map(group -> {
                    int currentCount = userGroupRepository.countByRunningGroup(group);
                    return new GroupDto.Response(
                            group.getId(),
                            group.getName(),
                            group.getDescription(),
                            group.isSecret(),
                            group.getOwner().getNickname(),
                            group.getMaxPeople(),
                            group.getTags(),
                            currentCount
                    );
                })
                .filter(dto -> {
                    if ("public".equals(type) && dto.isSecret()) return false;

                    if ("recruiting".equals(status)) {
                        // ★ 수정: 날짜 체크 삭제함. 인원 꽉 찼는지만 확인.
                        boolean isFull = dto.getMaxPeople() != null && dto.getCurrentPeople() >= dto.getMaxPeople();
                        if (isFull) return false;
                    }
                    return true;
                })
                .collect(Collectors.toList());
    }

    // 기존 getAllGroups는 getFilteredGroups가 대체하므로 삭제하거나 그냥 둬도 됨 (안 쓰임)
    @Transactional(readOnly = true)
    public List<GroupDto.Response> getAllGroups() {
        return getFilteredGroups(null, null, null);
    }

    // 5. 그룹 상세 조회 (설정 페이지용)
    @Transactional(readOnly = true)
    public GroupDto.DetailResponse getGroupDetail(String email, Long groupId) {
        RunningGroup group = groupRepository.findById(groupId)
                .orElseThrow(() -> new IllegalArgumentException("그룹 없음"));

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("유저 없음"));

        // 요청한 사람이 방장인지 확인
        boolean isOwner = group.getOwner().getId().equals(user.getId());

        return new GroupDto.DetailResponse(
                group.getId(),
                group.getName(),
                group.getDescription(),
                group.isSecret(),
                group.getAccessCode(), // 입장 코드 전달
                isOwner
        );
    }

    // 6. 그룹 수정 (방장만 가능)
    @Transactional
    public void updateGroup(String email, Long groupId, GroupDto.UpdateRequest request) {
        RunningGroup group = groupRepository.findById(groupId)
                .orElseThrow(() -> new IllegalArgumentException("그룹 없음"));
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("유저 없음"));

        // 방장 아니면 에러!
        if (!group.getOwner().getId().equals(user.getId())) {
            throw new IllegalArgumentException("방장만 수정할 수 있습니다.");
        }

        // 이름 변경 (JPA의 Dirty Checking으로 자동 저장됨)
        // RunningGroup 엔티티에 update 메서드 하나 만들어주면 더 좋음 (아래 참고)
        group.updateInfo(request.getGroupName(), request.getDescription());
    }

    // 7. 그룹 삭제 (방장만 가능)
    @Transactional
    public void deleteGroup(String email, Long groupId) {
        RunningGroup group = groupRepository.findById(groupId)
                .orElseThrow(() -> new IllegalArgumentException("그룹 없음"));
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("유저 없음"));

        if (!group.getOwner().getId().equals(user.getId())) {
            throw new IllegalArgumentException("방장만 삭제할 수 있습니다.");
        }

        // 그룹 삭제 (연관된 코스, 참여정보 등은 Cascade 설정에 따라 같이 지워짐)
        // Cascade 설정 안 했으면 여기서 courseRepository.deleteByGroup... 등을 먼저 해줘야 함
        // 일단 그룹 삭제 시도
        groupRepository.delete(group);
    }
}