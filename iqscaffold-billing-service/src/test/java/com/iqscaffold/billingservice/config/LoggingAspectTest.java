package com.iqscaffold.billingservice.config;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LoggingAspectTest {

    @Mock
    private ProceedingJoinPoint joinPoint;

    @Mock
    private Signature signature;

    private LoggingAspect loggingAspect;

    @BeforeEach
    void setUp() {
        loggingAspect = new LoggingAspect();
        MDC.clear();
    }

    @AfterEach
    void tearDown() {
        MDC.clear();
    }

    @Test
    void logPerformance_shouldLogOperationWithDefaultName() throws Throwable {
        // Given
        LoggingAspect.LogPerformance annotation = mock(LoggingAspect.LogPerformance.class);
        when(annotation.operation()).thenReturn("");
        when(annotation.logArgs()).thenReturn(false);
        when(annotation.logResult()).thenReturn(false);
        
        when(signature.toShortString()).thenReturn("TestClass.testMethod()");
        when(joinPoint.getSignature()).thenReturn(signature);
        when(joinPoint.proceed()).thenReturn("result");

        // When
        Object result = loggingAspect.logPerformance(joinPoint, annotation);

        // Then
        assertEquals("result", result);
        verify(joinPoint).proceed();
    }

    @Test
    void logPerformance_shouldLogOperationWithCustomName() throws Throwable {
        // Given
        LoggingAspect.LogPerformance annotation = mock(LoggingAspect.LogPerformance.class);
        when(annotation.operation()).thenReturn("CustomOperation");
        when(annotation.logArgs()).thenReturn(false);
        when(annotation.logResult()).thenReturn(false);
        
        when(joinPoint.proceed()).thenReturn("result");

        // When
        Object result = loggingAspect.logPerformance(joinPoint, annotation);

        // Then
        assertEquals("result", result);
        verify(joinPoint).proceed();
    }

    @Test
    void logPerformance_shouldLogArgumentsWhenEnabled() throws Throwable {
        // Given
        LoggingAspect.LogPerformance annotation = mock(LoggingAspect.LogPerformance.class);
        when(annotation.operation()).thenReturn("TestOp");
        when(annotation.logArgs()).thenReturn(true);
        when(annotation.logResult()).thenReturn(false);
        
        Object[] args = new Object[]{"arg1", "arg2"};
        when(joinPoint.getArgs()).thenReturn(args);
        when(joinPoint.proceed()).thenReturn("result");

        // When
        Object result = loggingAspect.logPerformance(joinPoint, annotation);

        // Then
        assertEquals("result", result);
        verify(joinPoint).getArgs();
    }

    @Test
    void logPerformance_shouldLogResultWhenEnabled() throws Throwable {
        // Given
        LoggingAspect.LogPerformance annotation = mock(LoggingAspect.LogPerformance.class);
        when(annotation.operation()).thenReturn("TestOp");
        when(annotation.logArgs()).thenReturn(false);
        when(annotation.logResult()).thenReturn(true);
        
        when(joinPoint.proceed()).thenReturn("result");

        // When
        Object result = loggingAspect.logPerformance(joinPoint, annotation);

        // Then
        assertEquals("result", result);
    }

    @Test
    void logPerformance_shouldHandleExceptionAndRethrow() throws Throwable {
        // Given
        LoggingAspect.LogPerformance annotation = mock(LoggingAspect.LogPerformance.class);
        lenient().when(annotation.operation()).thenReturn("FailingOp");
        lenient().when(annotation.logArgs()).thenReturn(false);
        lenient().when(annotation.logResult()).thenReturn(false);
        
        lenient().when(signature.toShortString()).thenReturn("TestClass.failingMethod()");
        lenient().when(joinPoint.getSignature()).thenReturn(signature);
        
        RuntimeException exception = new RuntimeException("Test error");
        when(joinPoint.proceed()).thenThrow(exception);

        // When & Then
        assertThrows(RuntimeException.class, () -> 
            loggingAspect.logPerformance(joinPoint, annotation)
        );
    }

    @Test
    void logBusinessEvent_shouldLogEventWithDefaultValues() throws Throwable {
        // Given
        LoggingAspect.LogBusinessEvent annotation = mock(LoggingAspect.LogBusinessEvent.class);
        when(annotation.eventType()).thenReturn("");
        when(annotation.description()).thenReturn("");
        
        when(signature.getName()).thenReturn("testMethod");
        when(signature.toShortString()).thenReturn("TestClass.testMethod()");
        when(joinPoint.getSignature()).thenReturn(signature);
        when(joinPoint.proceed()).thenReturn("result");

        // When
        Object result = loggingAspect.logBusinessEvent(joinPoint, annotation);

        // Then
        assertEquals("result", result);
        verify(joinPoint).proceed();
    }

    @Test
    void logBusinessEvent_shouldLogEventWithCustomValues() throws Throwable {
        // Given
        LoggingAspect.LogBusinessEvent annotation = mock(LoggingAspect.LogBusinessEvent.class);
        when(annotation.eventType()).thenReturn("PAYMENT_CREATED");
        when(annotation.description()).thenReturn("Payment was created successfully");
        
        when(joinPoint.proceed()).thenReturn("result");

        // When
        Object result = loggingAspect.logBusinessEvent(joinPoint, annotation);

        // Then
        assertEquals("result", result);
        verify(joinPoint).proceed();
    }

    @Test
    void logBusinessEvent_shouldHandleExceptionAndRethrow() throws Throwable {
        // Given
        LoggingAspect.LogBusinessEvent annotation = mock(LoggingAspect.LogBusinessEvent.class);
        when(annotation.eventType()).thenReturn("FAILING_EVENT");
        when(annotation.description()).thenReturn("This will fail");
        
        RuntimeException exception = new RuntimeException("Business error");
        when(joinPoint.proceed()).thenThrow(exception);

        // When & Then
        assertThrows(RuntimeException.class, () -> 
            loggingAspect.logBusinessEvent(joinPoint, annotation)
        );
    }

    @Test
    void logServiceMethods_shouldLogServiceCall() throws Throwable {
        // Given
        Object target = new Object();
        when(joinPoint.getTarget()).thenReturn(target);
        when(signature.getName()).thenReturn("testMethod");
        when(joinPoint.getSignature()).thenReturn(signature);
        when(joinPoint.proceed()).thenReturn("result");

        // When
        Object result = loggingAspect.logServiceMethods(joinPoint);

        // Then
        assertEquals("result", result);
        verify(joinPoint).proceed();
    }

    @Test
    void logServiceMethods_shouldLogSlowOperations() throws Throwable {
        // Given
        Object target = new Object();
        when(joinPoint.getTarget()).thenReturn(target);
        when(signature.getName()).thenReturn("slowMethod");
        when(joinPoint.getSignature()).thenReturn(signature);
        
        // Simulate slow operation
        when(joinPoint.proceed()).thenAnswer(invocation -> {
            Thread.sleep(1100);
            return "result";
        });

        // When
        Object result = loggingAspect.logServiceMethods(joinPoint);

        // Then
        assertEquals("result", result);
    }

    @Test
    void logServiceMethods_shouldHandleExceptionAndRethrow() throws Throwable {
        // Given
        Object target = new Object();
        when(joinPoint.getTarget()).thenReturn(target);
        when(signature.getName()).thenReturn("failingMethod");
        when(joinPoint.getSignature()).thenReturn(signature);
        
        RuntimeException exception = new RuntimeException("Service error");
        when(joinPoint.proceed()).thenThrow(exception);

        // When & Then
        assertThrows(RuntimeException.class, () -> 
            loggingAspect.logServiceMethods(joinPoint)
        );
    }

    @Test
    void logRepositoryMethods_shouldLogDatabaseOperation() throws Throwable {
        // Given
        Object target = new Object();
        when(joinPoint.getTarget()).thenReturn(target);
        when(signature.getName()).thenReturn("findById");
        when(joinPoint.getSignature()).thenReturn(signature);
        when(joinPoint.proceed()).thenReturn("result");

        // When
        Object result = loggingAspect.logRepositoryMethods(joinPoint);

        // Then
        assertEquals("result", result);
        verify(joinPoint).proceed();
    }

    @Test
    void logRepositoryMethods_shouldLogSlowDatabaseOperations() throws Throwable {
        // Given
        Object target = new Object();
        when(joinPoint.getTarget()).thenReturn(target);
        when(signature.getName()).thenReturn("slowQuery");
        when(joinPoint.getSignature()).thenReturn(signature);
        
        // Simulate slow database operation
        when(joinPoint.proceed()).thenAnswer(invocation -> {
            Thread.sleep(600);
            return "result";
        });

        // When
        Object result = loggingAspect.logRepositoryMethods(joinPoint);

        // Then
        assertEquals("result", result);
    }

    @Test
    void logRepositoryMethods_shouldHandleExceptionAndRethrow() throws Throwable {
        // Given
        Object target = new Object();
        when(joinPoint.getTarget()).thenReturn(target);
        when(signature.getName()).thenReturn("failingQuery");
        when(joinPoint.getSignature()).thenReturn(signature);
        
        RuntimeException exception = new RuntimeException("Database error");
        when(joinPoint.proceed()).thenThrow(exception);

        // When & Then
        assertThrows(RuntimeException.class, () -> 
            loggingAspect.logRepositoryMethods(joinPoint)
        );
    }
}
