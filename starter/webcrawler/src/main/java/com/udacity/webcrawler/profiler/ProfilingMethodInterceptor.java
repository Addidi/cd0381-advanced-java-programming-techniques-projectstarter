package com.udacity.webcrawler.profiler;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.Objects;

/**
 * A method interceptor that checks whether {@link Method}s are annotated with the {@link Profiled}
 * annotation. If they are, the method interceptor records how long the method invocation took.
 */
final class ProfilingMethodInterceptor implements InvocationHandler {

  private final Clock clock;
  private final Object delegate;
  private final ProfilingState profilingState;
  private final ZonedDateTime startTime;

  // TODO: You will need to add more instance fields and constructor arguments to this class.
  ProfilingMethodInterceptor(Clock clock,
                             Object delegate,
                             ProfilingState profilingState,
                             ZonedDateTime startTime) {
    this.clock = Objects.requireNonNull(clock);
    this.delegate = delegate;
    this.profilingState = profilingState;
    this.startTime = startTime;
  }

  @Override
  public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
    // TODO: This method interceptor should inspect the called method to see if it is a profiled
    //       method. For profiled methods, the interceptor should record the start time, then
    //       invoke the method using the object that is being profiled. Finally, for profiled
    //       methods, the interceptor should record how long the method call took, using the
    //       ProfilingState methods.
    Instant start = null;
    Object invoke;

    boolean profiled = method.getAnnotation(Profiled.class) != null;
    if(profiled)
      start = clock.instant();
    try {
      invoke = method.invoke(delegate, args);
    }catch(InvocationTargetException ex){
      throw  ex.getTargetException();
    }catch(IllegalAccessException ex){
      throw new RuntimeException(ex);
    }finally {
      if(profiled){
        Duration duration = Duration.between(start, clock.instant());
        profilingState.record(delegate.getClass(), method, duration);
      }
    }
    return invoke;
  }

  @Override
  public boolean equals(Object object){

    if(object == this) return true;
    if(!(object instanceof ProfilingMethodInterceptor)) return false;
    ProfilingMethodInterceptor methodInterceptor = (ProfilingMethodInterceptor) object;
    return Objects.equals(this.clock, methodInterceptor.clock)
            && Objects.equals(this.delegate,methodInterceptor.delegate)
            && Objects.equals(this.profilingState,methodInterceptor.profilingState)
            && Objects.equals(this.startTime,methodInterceptor.startTime);
  }
}
