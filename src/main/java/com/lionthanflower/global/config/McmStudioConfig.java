// MCM Studio 프레임 설정 객체를 Spring Bean으로 등록하는 구성
package com.lionthanflower.global.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(StudioFrameProperties.class)
public class McmStudioConfig {}
