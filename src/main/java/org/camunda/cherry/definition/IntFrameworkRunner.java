/* ******************************************************************** */
/*                                                                      */
/*  IntFrameworkRunner                                                  */
/*                                                                      */
/*  We want to identify which runner is part of the framework.          */
/* Only workers part of the framework must implement this interface     */
/* ******************************************************************** */
package org.camunda.cherry.definition;

import org.springframework.stereotype.Component;

@Component
public interface IntFrameworkRunner {
    public boolean isFrameworkRunner();
}
