package dev.erickmurakami.portfolio.infrastructure.ai;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.inject.Qualifier;
import static java.lang.annotation.ElementType.*;

@Qualifier
@Retention(RetentionPolicy.RUNTIME)
@Target({ FIELD, PARAMETER, METHOD })
public @interface QueryEmbedding {

}
