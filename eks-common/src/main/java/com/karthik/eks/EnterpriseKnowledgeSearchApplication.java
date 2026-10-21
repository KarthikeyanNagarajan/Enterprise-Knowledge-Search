package com.karthik.eks;

/**
 * Shared module marker.
 *
 * This class intentionally does not bootstrap a Spring application.
 * Application startup belongs to the dedicated runnable module, e.g. eks-app.
 */
public final class EnterpriseKnowledgeSearchApplication {

    private EnterpriseKnowledgeSearchApplication() {
    }
}
