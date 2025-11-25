package runtogether.server.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import runtogether.server.dto.CourseDto;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CourseService {

    // ★ AI 추천 코스 목록 조회 (하드코딩 버전)
    public List<CourseDto.Response> getRecommendedCourses() {
        List<CourseDto.Response> list = new ArrayList<>();

        // 1. 석촌 호수 루프
        list.add(new CourseDto.Response(
                -1L,
                "석촌 호수 루프",
                "신호등이 없고 바닥이 우레탄이라 부상 위험이 적어요! 야경이 예뻐 밤에 뛰기 아주 좋습니다!",
                2.5,
                20,
                "[{lat:37.50,lng:127.05}, {lat:37.51,lng:127.06}]" // 임시 좌표
        ));

        // 2. 여의도 고구마 코스
        list.add(new CourseDto.Response(
                -2L,
                "여의도 고구마 코스",
                "지도 앱으로 경로를 그리면 고구마 모양이 나와서 러너들 사이에서 매우 유명한 인증샷 코스예요!",
                8.2,
                60,
                "[{lat:37.52,lng:126.92}, {lat:37.53,lng:126.93}]"
        ));

        // 3. 한강 시민공원 코스
        list.add(new CourseDto.Response(
                -3L,
                "한강 시민공원 코스",
                "여의도 공원 출발 -> 반포지구 -> 잠실지구 근처 반환 -> 여의도 복귀",
                21.09,
                120,
                "[{lat:37.53,lng:126.92}, {lat:37.51,lng:126.99}]"
        ));

        return list;
    }
}