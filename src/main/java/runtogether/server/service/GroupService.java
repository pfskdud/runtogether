package runtogether.server.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import runtogether.server.domain.*;
import runtogether.server.dto.GroupDto;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GroupService {

    private final RunningGroupRepository groupRepository;
    private final UserGroupRepository userGroupRepository;
    private final UserRepository userRepository;
    private final CourseRepository courseRepository;
    private final RecordRepository recordRepository; // ★ 추가: 내 기록 조회용

    // 1. 그룹 생성
    @Transactional
    public Long createGroup(String email, GroupDto.CreateRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("유저 없음"));

        RunningGroup group = new RunningGroup(
                request.getGroupName(),
                request.getDescription(),
                request.isSecret(),
                request.isSearchable(),
                request.getMaxPeople(),
                request.getTags(),
                user
        );

        RunningGroup savedGroup = groupRepository.save(group);
        userGroupRepository.save(new UserGroup(user, savedGroup));

        return savedGroup.getId();
    }

    // 헬퍼 메서드
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
                request.getStartDate(), // 날짜 저장
                request.getEndDate(),   // 날짜 저장
                group
        );
        courseRepository.save(course);
    }

    // 3. 그룹 참여
    @Transactional
    public String joinGroup(String email, Long groupId, String inputCode) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("유저 없음"));
        RunningGroup group = groupRepository.findById(groupId)
                .orElseThrow(() -> new IllegalArgumentException("그룹 없음"));

        if (userGroupRepository.existsByUserAndRunningGroup(user, group)) {
            throw new IllegalArgumentException("이미 가입된 그룹입니다.");
        }

        if (group.isSecret()) {
            if (inputCode == null || !inputCode.equals(group.getAccessCode())) {
                throw new IllegalArgumentException("입장 코드가 올바르지 않습니다.");
            }
        }

        // 인원 마감 체크
        int currentCount = userGroupRepository.countByRunningGroup(group);
        if (group.getMaxPeople() != null && currentCount >= group.getMaxPeople()) {
            throw new IllegalArgumentException("정원이 초과되어 가입할 수 없습니다.");
        }

        userGroupRepository.save(new UserGroup(user, group));
        return "그룹 가입 완료!";
    }

    // 4. 그룹 목록 조회 (검색 + 필터링)
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

                    // 모집중 필터: 인원수만 체크 (날짜는 코스별로 다르니 생략)
                    if ("recruiting".equals(status)) {
                        boolean isFull = dto.getMaxPeople() != null && dto.getCurrentPeople() >= dto.getMaxPeople();
                        if (isFull) return false;
                    }
                    return true;
                })
                .collect(Collectors.toList());
    }

    // 안 쓰는 getAllGroups는 삭제해도 됨

    // 5. 그룹 상세 조회 (설정용)
    @Transactional(readOnly = true)
    public GroupDto.DetailResponse getGroupDetail(String email, Long groupId) {
        RunningGroup group = groupRepository.findById(groupId)
                .orElseThrow(() -> new IllegalArgumentException("그룹 없음"));
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("유저 없음"));

        boolean isOwner = group.getOwner().getId().equals(user.getId());

        return new GroupDto.DetailResponse(
                group.getId(),
                group.getName(),
                group.getDescription(),
                group.isSecret(),
                group.getAccessCode(),
                isOwner
        );
    }

    // 6. 그룹 수정
    @Transactional
    public void updateGroup(String email, Long groupId, GroupDto.UpdateRequest request) {
        RunningGroup group = groupRepository.findById(groupId)
                .orElseThrow(() -> new IllegalArgumentException("그룹 없음"));
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("유저 없음"));

        if (!group.getOwner().getId().equals(user.getId())) {
            throw new IllegalArgumentException("방장만 수정할 수 있습니다.");
        }
        group.updateInfo(request.getGroupName(), request.getDescription());
    }

    // 7. 그룹 삭제
    @Transactional
    public void deleteGroup(String email, Long groupId) {
        RunningGroup group = groupRepository.findById(groupId)
                .orElseThrow(() -> new IllegalArgumentException("그룹 없음"));
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("유저 없음"));

        if (!group.getOwner().getId().equals(user.getId())) {
            throw new IllegalArgumentException("방장만 삭제할 수 있습니다.");
        }
        groupRepository.delete(group);
    }

    // ★ [추가] 8. 그룹 메인 화면 조회
    @Transactional(readOnly = true)
    public GroupDto.MainResponse getGroupMain(String email, Long groupId) {
        User user = userRepository.findByEmail(email).orElseThrow(() -> new IllegalArgumentException("유저 없음"));
        RunningGroup group = groupRepository.findById(groupId).orElseThrow(() -> new IllegalArgumentException("그룹 없음"));

        // 코스 정보 가져오기 (첫 번째 코스를 메인으로 사용)
        // 주의: CourseRepository에 findByRunningGroup 메서드가 있어야 함!
        Course mainCourse = courseRepository.findByRunningGroup(group).stream().findFirst()
                .orElse(new Course("코스 없음", 0.0, 0, null, null, null, null, group));

        double myTotalDistance = 0.0; // 기록 합산 로직 (일단 0)

        // D-Day 및 기간 계산 (코스 기준)
        long dDay = 0;
        String datePeriod = "기간 미정";
        if (mainCourse.getEndDate() != null) {
            dDay = ChronoUnit.DAYS.between(LocalDate.now(), mainCourse.getEndDate());
            datePeriod = mainCourse.getStartDate() + " - " + mainCourse.getEndDate();
        }

        return new GroupDto.MainResponse(
                group.getName(),
                mainCourse.getTitle(),
                datePeriod,
                dDay,
                user.getNickname(),
                myTotalDistance,
                mainCourse.getDistance(),
                user.getProfileImageUrl()
        );
    }
}