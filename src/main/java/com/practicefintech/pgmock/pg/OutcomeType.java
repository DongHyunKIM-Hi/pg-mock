package com.practicefintech.pgmock.pg;

/**
 * 가상 결제대행사가 환불 요청 하나를 처리한 결과 유형.
 * SUCCESS/FAIL은 정상 응답(200), ERROR는 내부 오류(500), SLOW는 오래 걸리는 실패,
 * SLOW_BUT_PROCESSED는 오래 걸리지만 실제로는 성공 처리되는 함정 케이스다.
 */
public enum OutcomeType {
    SUCCESS,
    FAIL,
    ERROR,
    SLOW,
    SLOW_BUT_PROCESSED
}
