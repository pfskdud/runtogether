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

    // ★ [추가] 경로 검색 서비스 (Mock Data)
    public CourseDto.Response searchRoute(CourseDto.RouteRequest request) {

        String start = request.getStartLocation();
        String end = request.getEndLocation();

        // 나중에는 여기서 TMap API를 호출해서 진짜 길 찾기 결과를 받아오면 됩니다.
        // 지금은 "숙명여자대학교" -> "여의도공원" 예시 데이터를 반환합니다.

        String description = """
                숙명여자대학교 정문 (고지대)
                → 효창공원 (녹지 구간, 가벼운 조깅)
                → 공덕역 ~ 마포대교 북단 진입로 (도심 구간)
                → 마포대교 횡단 (하이라이트, 바람을 맞으며 한강 뷰)
                → 여의도 한강공원 물빛광장 (평지 스피드 구간)
                → 샛강생태공원 산책로 (자연 뷰)
                → 여의도 공원 문화의 마당 (탁 트인 광장)
                """;

        // 실제로는 API가 주는 좌표 리스트가 들어갑니다.
        String mockPathData = "[{lat:37.546,lng:126.964}, {lat:37.542,lng:126.961}, {lat:37.533,lng:126.940}, {lat:37.528,lng:126.933}]";

        return new CourseDto.Response(
                -999L, // 저장된 코스가 아니므로 임시 ID
                start + " ~ " + end + " 러닝 코스", // 제목 자동 생성
                description,
                10.0, // 거리 (10km)
                60,   // 예상 시간 (60분)
                mockPathData
        );
    }
}