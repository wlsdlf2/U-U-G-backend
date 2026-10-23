package com.aigo.speech.interview.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.aigo.speech.answer.repository.AnswerAnalysisRepository;
import com.aigo.speech.interview.entity.InterviewReport;
import com.aigo.speech.interview.entity.InterviewSession;
import com.aigo.speech.interview.exception.InterviewReportFailedException;
import com.aigo.speech.interview.exception.InterviewReportPendingException;
import com.aigo.speech.interview.repository.InterviewReportRepository;
import com.aigo.speech.interview.repository.InterviewSessionRepository;
import com.aigo.speech.jobposting.entity.JobPosting;
import com.aigo.speech.question.repository.InterviewQuestionRepository;
import com.aigo.speech.user.entity.User;
import com.aigo.speech.user.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class InterviewReportServiceTest {

	@Mock private UserRepository userRepository;
	@Mock private InterviewSessionRepository sessionRepository;
	@Mock private InterviewReportRepository reportRepository;
	@Mock private AnswerAnalysisRepository analysisRepository;
	@Mock private InterviewQuestionRepository questionRepository;
	@Mock private InterviewSessionService sessionService;

	@InjectMocks
	private InterviewReportService reportService;

	private static final UUID USER_UUID = UUID.randomUUID();
	private static final UUID SESSION_UUID = UUID.randomUUID();

	private InterviewSession session;

	@BeforeEach
	void setUp() {
		User user = User.builder().email("test@example.com").build();
		ReflectionTestUtils.setField(user, "uuid", USER_UUID);
		JobPosting jobPosting = JobPosting.pending(user, "https://wanted.co.kr/wd/1");

		session = new InterviewSession(user, jobPosting, false, null);
		given(sessionService.findByUuid(SESSION_UUID)).willReturn(session);
	}

	// ======================== getReportDetail ========================

	@Test
	@DisplayName("리포트가 아직 없으면 분석 진행 중 예외가 발생한다")
	void getReportDetail_noReport_throwsPending() {
		given(reportRepository.findBySession(session)).willReturn(Optional.empty());

		assertThatThrownBy(() -> reportService.getReportDetail(SESSION_UUID, USER_UUID.toString()))
			.isInstanceOf(InterviewReportPendingException.class);
	}

	@Test
	@DisplayName("리포트가 생성 중이면 분석 진행 중 예외가 발생한다")
	void getReportDetail_generating_throwsPending() {
		given(reportRepository.findBySession(session)).willReturn(Optional.of(new InterviewReport(session)));

		assertThatThrownBy(() -> reportService.getReportDetail(SESSION_UUID, USER_UUID.toString()))
			.isInstanceOf(InterviewReportPendingException.class);
	}

	@Test
	@DisplayName("리포트 생성에 실패했으면 분석 실패 예외가 발생한다")
	void getReportDetail_failed_throwsFailed() {
		InterviewReport report = new InterviewReport(session);
		report.fail();
		given(reportRepository.findBySession(session)).willReturn(Optional.of(report));

		assertThatThrownBy(() -> reportService.getReportDetail(SESSION_UUID, USER_UUID.toString()))
			.isInstanceOf(InterviewReportFailedException.class)
			.hasMessage("리포트 생성에 실패했습니다.");
	}
}
