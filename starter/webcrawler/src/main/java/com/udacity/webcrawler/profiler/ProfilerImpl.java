package com.udacity.webcrawler.profiler;

import javax.inject.Inject;
import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Clock;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import static java.time.format.DateTimeFormatter.RFC_1123_DATE_TIME;

/**
 * Concrete implementation of the {@link Profiler}.
 */
final class ProfilerImpl implements Profiler {

  private final Clock clock;
  private final ProfilingState state = new ProfilingState();
  private final ZonedDateTime startTime;

  @Inject
  ProfilerImpl(Clock clock) {
    this.clock = Objects.requireNonNull(clock);
    this.startTime = ZonedDateTime.now(clock);
  }

  @Override
  public <T> T wrap(Class<T> klass, T delegate) {
    Objects.requireNonNull(klass);

    // TODO: Use a dynamic proxy (java.lang.reflect.Proxy) to "wrap" the delegate in a
    //       ProfilingMethodInterceptor and return a dynamic proxy from this method.
    //       See https://docs.oracle.com/javase/10/docs/api/java/lang/reflect/Proxy.html.

    if(!isClassProfiled(klass)){
      throw new IllegalArgumentException("Class "+ klass.getName() + " does not contain any profiled method");
    }

    ProfilingMethodInterceptor methodInterceptor = new ProfilingMethodInterceptor(clock, delegate, state, startTime);

    Object proxy = Proxy.newProxyInstance(
            ProfilerImpl.class.getClassLoader(),
            new Class<?>[]{klass},
            methodInterceptor
    );
    return (T)proxy;
  }
  @Profiled
  public Boolean isClassProfiled(Class<?> klass){
    List<Method> methods = Arrays.asList(klass.getDeclaredMethods());
    if(methods.isEmpty()) return false;
    for(Method method: methods){
      if(method.getAnnotation(Profiled.class) != null) return true;
    }
    return false;
  }

  @Override
  public void writeData(Path path) {
    // TODO: Write the ProfilingState data to the given file path. If a file already exists at that
    //       path, the new data should be appended to the existing file.
    try(Writer writer = Files.newBufferedWriter(path, StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {
      writeData(writer);
      writer.flush();
    }catch (IOException ioException){
      ioException.printStackTrace();
    }
  }
  @Override
  public void writeData(Writer writer){
    try{
      writer.write("Run at " + RFC_1123_DATE_TIME.format(startTime));
      writer.write(System.lineSeparator());
      state.write(writer);
      writer.write(System.lineSeparator());
      writer.flush();
    }catch (IOException e){
      e.printStackTrace();
    }
  }
}
