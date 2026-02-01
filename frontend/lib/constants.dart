import 'package:flutter/material.dart';

// 디자인에 있는 오렌지색
const Color primaryColor = Color(0xFFFF7E36);

// 백엔드 ngrok 주소
const String baseUrl = 'https://byssal-nondyspeptically-roseann.ngrok-free.dev';

// Auth
const String loginUrl = '$baseUrl/api/v1/auth/login';
const String registerUrl = '$baseUrl/api/v1/auth/register';
const String profileUrl = '$baseUrl/api/v1/auth/profile';
const String checkEmailUrl = '$baseUrl/api/v1/auth/check-email';

// Group
const String groupUrl = '$baseUrl/api/v1/groups'; // 목록 조회(GET) 및 생성(POST)
const String myGroupUrl = '$baseUrl/api/v1/groups/my'; // 내 대회 목록
// ★ [추가됨] 코드 입력해서 가입하는 주소
const String groupJoinCodeUrl = '$baseUrl/api/v1/groups/join/code';

// Course & Record
const String courseUrl = '$baseUrl/api/v1/courses';
const String recordUrl = '$baseUrl/api/v1/records';

// [코스 관련 API]
const String aiPersonalUrl = '$baseUrl/api/v1/courses/recommendations/personalized';
const String courseSearchUrl = '$baseUrl/api/v1/courses/search';
const String rankingBaseUrl = '$baseUrl/api/v1/courses';