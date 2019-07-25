/*
 *
 *  Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 *  or more contributor license agreements. Licensed under the Elastic License;
 *  you may not use this file except in compliance with the Elastic License.
 *
 */

package org.elasticsearch.xpack.core.security.authz.privilege;

import org.elasticsearch.action.admin.cluster.repositories.get.GetRepositoriesAction;
import org.elasticsearch.action.admin.cluster.snapshots.create.CreateSnapshotAction;
import org.elasticsearch.action.admin.cluster.snapshots.get.GetSnapshotsAction;
import org.elasticsearch.action.admin.cluster.snapshots.status.SnapshotsStatusAction;
import org.elasticsearch.action.admin.cluster.state.ClusterStateAction;
import org.elasticsearch.common.Strings;
import org.elasticsearch.xpack.core.indexlifecycle.action.GetLifecycleAction;
import org.elasticsearch.xpack.core.indexlifecycle.action.GetStatusAction;
import org.elasticsearch.xpack.core.indexlifecycle.action.StartILMAction;
import org.elasticsearch.xpack.core.indexlifecycle.action.StopILMAction;
import org.elasticsearch.xpack.core.security.action.token.InvalidateTokenAction;
import org.elasticsearch.xpack.core.security.action.token.RefreshTokenAction;
import org.elasticsearch.xpack.core.security.action.user.HasPrivilegesAction;
import org.elasticsearch.xpack.core.snapshotlifecycle.action.GetSnapshotLifecycleAction;

import java.util.Collections;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Translates cluster privilege names into concrete implementations
 */
public class ClusterPrivilegeResolver {
    // shared automatons
    private static final Set<String> MANAGE_SECURITY_PATTERN = Set.of("cluster:admin/xpack/security/*");
    private static final Set<String> MANAGE_SAML_PATTERN = Set.of("cluster:admin/xpack/security/saml/*",
        InvalidateTokenAction.NAME, RefreshTokenAction.NAME);
    private static final Set<String> MANAGE_OIDC_PATTERN = Set.of("cluster:admin/xpack/security/oidc/*");
    private static final Set<String> MANAGE_TOKEN_PATTERN = Set.of("cluster:admin/xpack/security/token/*");
    private static final Set<String> MANAGE_API_KEY_PATTERN = Set.of("cluster:admin/xpack/security/api_key/*");
    private static final Set<String> MONITOR_PATTERN = Set.of("cluster:monitor/*");
    private static final Set<String> MONITOR_ML_PATTERN = Set.of("cluster:monitor/xpack/ml/*");
    private static final Set<String> MONITOR_DATA_FRAME_PATTERN = Set.of("cluster:monitor/data_frame/*");
    private static final Set<String> MONITOR_WATCHER_PATTERN = Set.of("cluster:monitor/xpack/watcher/*");
    private static final Set<String> MONITOR_ROLLUP_PATTERN = Set.of("cluster:monitor/xpack/rollup/*");
    private static final Set<String> ALL_CLUSTER_PATTERN = Set.of("cluster:*", "indices:admin/template/*");
    private static final Set<String> MANAGE_ML_PATTERN = Set.of("cluster:admin/xpack/ml/*", "cluster:monitor/xpack/ml/*");
    private static final Set<String> MANAGE_DATA_FRAME_PATTERN = Set.of("cluster:admin/data_frame/*", "cluster:monitor/data_frame/*");
    private static final Set<String> MANAGE_WATCHER_PATTERN = Set.of("cluster:admin/xpack/watcher/*", "cluster:monitor/xpack/watcher/*");
    private static final Set<String> TRANSPORT_CLIENT_PATTERN = Set.of("cluster:monitor/nodes/liveness", "cluster:monitor/state");
    private static final Set<String> MANAGE_IDX_TEMPLATE_PATTERN = Set.of("indices:admin/template/*");
    private static final Set<String> MANAGE_INGEST_PIPELINE_PATTERN = Set.of("cluster:admin/ingest/pipeline/*");
    private static final Set<String> MANAGE_ROLLUP_PATTERN = Set.of("cluster:admin/xpack/rollup/*", "cluster:monitor/xpack/rollup/*");
    private static final Set<String> MANAGE_CCR_PATTERN =
        Set.of("cluster:admin/xpack/ccr/*", ClusterStateAction.NAME, HasPrivilegesAction.NAME);
    private static final Set<String> CREATE_SNAPSHOT_PATTERN = Set.of(CreateSnapshotAction.NAME, SnapshotsStatusAction.NAME + "*",
        GetSnapshotsAction.NAME, SnapshotsStatusAction.NAME, GetRepositoriesAction.NAME);
    private static final Set<String> READ_CCR_PATTERN = Set.of(ClusterStateAction.NAME, HasPrivilegesAction.NAME);
    private static final Set<String> MANAGE_ILM_PATTERN = Set.of("cluster:admin/ilm/*");
    private static final Set<String> READ_ILM_PATTERN = Set.of(GetLifecycleAction.NAME, GetStatusAction.NAME);
    private static final Set<String> MANAGE_SLM_PATTERN =
        Set.of("cluster:admin/slm/*", StartILMAction.NAME, StopILMAction.NAME, GetStatusAction.NAME);
    private static final Set<String> READ_SLM_PATTERN = Set.of(GetSnapshotLifecycleAction.NAME, GetStatusAction.NAME);

    public static final NameableClusterPrivilege NONE = new ActionClusterPrivilege("none", Set.of(), Set.of());
    public static final NameableClusterPrivilege ALL = new ActionClusterPrivilege("all", ALL_CLUSTER_PATTERN);
    public static final NameableClusterPrivilege MONITOR = new ActionClusterPrivilege("monitor", MONITOR_PATTERN);
    public static final NameableClusterPrivilege MONITOR_ML = new ActionClusterPrivilege("monitor_ml", MONITOR_ML_PATTERN);
    public static final NameableClusterPrivilege MONITOR_DATA_FRAME =
        new ActionClusterPrivilege("monitor_data_frame_transforms", MONITOR_DATA_FRAME_PATTERN);
    public static final NameableClusterPrivilege MONITOR_WATCHER = new ActionClusterPrivilege("monitor_watcher", MONITOR_WATCHER_PATTERN);
    public static final NameableClusterPrivilege MONITOR_ROLLUP = new ActionClusterPrivilege("monitor_rollup", MONITOR_ROLLUP_PATTERN);
    public static final NameableClusterPrivilege MANAGE = new ActionClusterPrivilege("manage",
        ALL_CLUSTER_PATTERN, MANAGE_SECURITY_PATTERN);
    public static final NameableClusterPrivilege MANAGE_ML = new ActionClusterPrivilege("manage_ml", MANAGE_ML_PATTERN);
    public static final NameableClusterPrivilege MANAGE_DATA_FRAME =
        new ActionClusterPrivilege("manage_data_frame_transforms", MANAGE_DATA_FRAME_PATTERN);
    public static final NameableClusterPrivilege MANAGE_TOKEN = new ActionClusterPrivilege("manage_token", MANAGE_TOKEN_PATTERN);
    public static final NameableClusterPrivilege MANAGE_WATCHER = new ActionClusterPrivilege("manage_watcher", MANAGE_WATCHER_PATTERN);
    public static final NameableClusterPrivilege MANAGE_ROLLUP = new ActionClusterPrivilege("manage_rollup", MANAGE_ROLLUP_PATTERN);
    public static final NameableClusterPrivilege MANAGE_IDX_TEMPLATES =
        new ActionClusterPrivilege("manage_index_templates", MANAGE_IDX_TEMPLATE_PATTERN);
    public static final NameableClusterPrivilege MANAGE_INGEST_PIPELINES =
        new ActionClusterPrivilege("manage_ingest_pipelines", MANAGE_INGEST_PIPELINE_PATTERN);
    public static final NameableClusterPrivilege TRANSPORT_CLIENT = new ActionClusterPrivilege("transport_client",
        TRANSPORT_CLIENT_PATTERN);
    public static final NameableClusterPrivilege MANAGE_SECURITY = new ActionClusterPrivilege("manage_security", MANAGE_SECURITY_PATTERN);
    public static final NameableClusterPrivilege MANAGE_SAML = new ActionClusterPrivilege("manage_saml", MANAGE_SAML_PATTERN);
    public static final NameableClusterPrivilege MANAGE_OIDC = new ActionClusterPrivilege("manage_oidc", MANAGE_OIDC_PATTERN);
    public static final NameableClusterPrivilege MANAGE_API_KEY = new ActionClusterPrivilege("manage_api_key", MANAGE_API_KEY_PATTERN);
    public static final NameableClusterPrivilege MANAGE_PIPELINE = new ActionClusterPrivilege("manage_pipeline", Set.of("cluster:admin" +
        "/ingest/pipeline/*"));
    public static final NameableClusterPrivilege MANAGE_CCR =            new ActionClusterPrivilege("manage_ccr", MANAGE_CCR_PATTERN);
    public static final NameableClusterPrivilege READ_CCR = new ActionClusterPrivilege("read_ccr", READ_CCR_PATTERN);
    public static final NameableClusterPrivilege CREATE_SNAPSHOT = new ActionClusterPrivilege("create_snapshot", CREATE_SNAPSHOT_PATTERN);
    public static final NameableClusterPrivilege MANAGE_ILM = new ActionClusterPrivilege("manage_ilm", MANAGE_ILM_PATTERN);
    public static final NameableClusterPrivilege READ_ILM = new ActionClusterPrivilege("read_ilm", READ_ILM_PATTERN);
    public static final NameableClusterPrivilege MANAGE_SLM = new ActionClusterPrivilege("manage_slm", MANAGE_SLM_PATTERN);
    public static final NameableClusterPrivilege READ_SLM = new ActionClusterPrivilege("read_slm", READ_SLM_PATTERN);

    private static final Map<String, NameableClusterPrivilege> VALUES = Stream.of(
        NONE,
        ALL,
        MONITOR,
        MONITOR_ML,
        MONITOR_DATA_FRAME,
        MONITOR_WATCHER,
        MONITOR_ROLLUP,
        MANAGE,
        MANAGE_ML,
        MANAGE_DATA_FRAME,
        MANAGE_TOKEN,
        MANAGE_WATCHER,
        MANAGE_IDX_TEMPLATES,
        MANAGE_INGEST_PIPELINES,
        TRANSPORT_CLIENT,
        MANAGE_SECURITY,
        MANAGE_SAML,
        MANAGE_OIDC,
        MANAGE_API_KEY,
        MANAGE_PIPELINE,
        MANAGE_ROLLUP,
        MANAGE_CCR,
        READ_CCR,
        CREATE_SNAPSHOT,
        MANAGE_ILM,
        READ_ILM,
        MANAGE_SLM,
        READ_SLM).collect(Collectors.toUnmodifiableMap(NameableClusterPrivilege::name, Function.identity()));

    public static NameableClusterPrivilege resolve(String name) {
        name = Objects.requireNonNull(name).toLowerCase(Locale.ROOT);
        if (isClusterAction(name)) {
            return new ActionClusterPrivilege(name, Set.of(name));
        }
        final NameableClusterPrivilege fixedPrivilege = VALUES.get(name);
        if (fixedPrivilege != null) {
            return fixedPrivilege;
        }
        throw new IllegalArgumentException("unknown cluster privilege [" + name + "]. a privilege must be either " +
            "one of the predefined fixed cluster privileges [" +
            Strings.collectionToCommaDelimitedString(VALUES.entrySet()) + "] or a pattern over one of the available " +
            "cluster actions");

    }

    public static Set<String> names() {
        return Collections.unmodifiableSet(VALUES.keySet());
    }

    public static boolean isClusterAction(String actionName) {
        return actionName.startsWith("cluster:") || actionName.startsWith("indices:admin/template/");
    }
}
