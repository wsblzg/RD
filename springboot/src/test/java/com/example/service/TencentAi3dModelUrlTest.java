package com.example.service;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ResponseEntity;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TencentAi3dModelUrlTest {

    @Test
    void modelFileProxyExposesStreamingResponseWithRangeSupport() throws Exception {
        Method method = TencentAi3dService.class.getMethod(
                "fetchModelFile",
                String.class,
                String.class
        );
        ParameterizedType returnType = (ParameterizedType) method.getGenericReturnType();

        assertEquals(ResponseEntity.class, returnType.getRawType());
        assertEquals(InputStreamResource.class, returnType.getActualTypeArguments()[0]);
    }

    @Test
    void proxiesTemporaryTencentModelUrlThroughSameOriginEndpoint() {
        String remoteUrl = "https://example.tencentcos.cn/model.glb?q-signature=test";

        assertEquals(
                "/api/ceramic-creation/model-file?url="
                        + URLEncoder.encode(remoteUrl, StandardCharsets.UTF_8),
                TencentAi3dService.proxyModelUrl(remoteUrl)
        );
    }

    @Test
    void keepsNonTencentModelUrlUnchanged() {
        String ossUrl = "https://example.oss-cn-shenzhen.aliyuncs.com/model.glb";

        assertEquals(ossUrl, TencentAi3dService.proxyModelUrl(ossUrl));
    }
}
