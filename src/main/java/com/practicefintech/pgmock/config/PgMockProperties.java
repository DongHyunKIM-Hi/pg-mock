package com.practicefintech.pgmock.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 가상 결제대행사의 동작을 조절하는 설정. application.yml의 pgmock.* 값과 바인딩된다.
 * randomSeed를 주면 결과가 재현 가능해진다(수동 테스트용). 운영·평가에서는 보통 비워 둔다.
 */
@ConfigurationProperties(prefix = "pgmock")
public record PgMockProperties(Weights weights, Delay delay, Long randomSeed) {

    public record Weights(int success, int fail, int error, int slow, int slowButProcessed) {
        public int total() {
            return success + fail + error + slow + slowButProcessed;
        }
    }

    public record Delay(int normalMinMs, int normalMaxMs, int slowMs) {
    }
}
