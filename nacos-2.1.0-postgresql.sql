create table if not exists public.config_info
(
    id                 bigserial
        primary key,
    data_id            varchar(255)                           not null,
    group_id           varchar(255) default NULL::character varying,
    content            text                                   not null,
    md5                varchar(32)  default NULL::character varying,
    gmt_create         timestamp    default CURRENT_TIMESTAMP not null,
    gmt_modified       timestamp    default CURRENT_TIMESTAMP not null,
    src_user           text,
    src_ip             varchar(50)  default NULL::character varying,
    app_name           varchar(128) default NULL::character varying,
    tenant_id          varchar(128) default ''::character varying,
    c_desc             varchar(256) default NULL::character varying,
    c_use              varchar(64)  default NULL::character varying,
    effect             varchar(64)  default NULL::character varying,
    type               varchar(64)  default NULL::character varying,
    c_schema           text,
    encrypted_data_key text,
    constraint uk_configinfo_datagrouptenant
        unique (data_id, group_id, tenant_id)
);

create table if not exists public.config_info_aggr
(
    id           bigserial
        primary key,
    data_id      varchar(255)                           not null,
    group_id     varchar(255)                           not null,
    datum_id     varchar(255)                           not null,
    content      text                                   not null,
    gmt_modified timestamp    default CURRENT_TIMESTAMP not null,
    app_name     varchar(128) default NULL::character varying,
    tenant_id    varchar(128) default ''::character varying,
    constraint uk_configinfoaggr_datagrouptenantdatum
        unique (data_id, group_id, tenant_id, datum_id)
);

comment on table public.config_info_aggr is '增加租户字段';

comment on column public.config_info_aggr.content is '内容';

comment on column public.config_info_aggr.gmt_modified is '修改时间';

comment on column public.config_info_aggr.tenant_id is '租户字段';

create table if not exists public.config_info_beta
(
    id                 bigserial
        primary key,
    data_id            varchar(255)                            not null,
    group_id           varchar(128)                            not null,
    app_name           varchar(128)  default NULL::character varying,
    content            text                                    not null,
    beta_ips           varchar(1024) default NULL::character varying,
    md5                varchar(32)   default NULL::character varying,
    gmt_create         timestamp     default CURRENT_TIMESTAMP not null,
    gmt_modified       timestamp     default CURRENT_TIMESTAMP not null,
    src_user           text,
    src_ip             varchar(50)   default NULL::character varying,
    tenant_id          varchar(128)  default ''::character varying,
    encrypted_data_key text,
    constraint uk_configinfobeta_datagrouptenant
        unique (data_id, group_id, tenant_id)
);

comment on column public.config_info_beta.gmt_create is '创建时间';

comment on column public.config_info_beta.gmt_modified is '修改时间';

comment on column public.config_info_beta.tenant_id is '租户字段';

create table if not exists public.config_info_tag
(
    id           bigserial
        primary key,
    data_id      varchar(255)                           not null,
    group_id     varchar(128)                           not null,
    tenant_id    varchar(128) default ''::character varying,
    tag_id       varchar(128)                           not null,
    app_name     varchar(128) default NULL::character varying,
    content      text                                   not null,
    md5          varchar(32)  default NULL::character varying,
    gmt_create   timestamp    default CURRENT_TIMESTAMP not null,
    gmt_modified timestamp    default CURRENT_TIMESTAMP not null,
    src_user     text,
    src_ip       varchar(50)  default NULL::character varying,
    constraint uk_configinfotag_datagrouptenanttag
        unique (data_id, group_id, tenant_id, tag_id)
);

comment on column public.config_info_tag.tenant_id is '租户字段';

comment on column public.config_info_tag.gmt_create is '创建时间';

comment on column public.config_info_tag.gmt_modified is '修改时间';

create table if not exists public.config_tags_relation
(
    id        bigint       not null,
    tag_name  varchar(128) not null,
    tag_type  varchar(64)  default NULL::character varying,
    data_id   varchar(255) not null,
    group_id  varchar(128) not null,
    tenant_id varchar(128) default ''::character varying,
    nid       bigserial
        primary key,
    constraint uk_configtagrelation_configidtag
        unique (id, tag_name, tag_type)
);

create index if not exists idx_tenant_id
    on public.config_tags_relation (tenant_id);

create table if not exists public.group_capacity
(
    id                bigserial
        primary key,
    group_id          varchar(128) default ''::character varying not null
        constraint uk_group_id
            unique,
    quota             integer      default 0                     not null,
    usage             integer      default 0                     not null,
    max_size          integer      default 0                     not null,
    max_aggr_count    integer      default 0                     not null,
    max_aggr_size     integer      default 0                     not null,
    max_history_count integer      default 0                     not null,
    gmt_create        timestamp    default CURRENT_TIMESTAMP     not null,
    gmt_modified      timestamp    default CURRENT_TIMESTAMP     not null
);

comment on table public.group_capacity is '集群、各Group容量信息表';

comment on column public.group_capacity.id is '主键id';

comment on column public.group_capacity.group_id is 'Group ID，空字符表示整个集群';

comment on column public.group_capacity.quota is '配额，0表示使用默认值';

comment on column public.group_capacity.usage is '使用量';

comment on column public.group_capacity.max_size is '单个配置大小上限，单位为字节，0表示使用默认值';

comment on column public.group_capacity.max_aggr_count is '聚合子配置最大个数，，0表示使用默认值';

comment on column public.group_capacity.max_aggr_size is '单个聚合数据的子配置大小上限，单位为字节，0表示使用默认值';

comment on column public.group_capacity.max_history_count is '最大变更历史数量';

comment on column public.group_capacity.gmt_create is '创建时间';

comment on column public.group_capacity.gmt_modified is '修改时间';

create table if not exists public.his_config_info
(
    id                 bigint                                 not null,
    nid                bigserial
        primary key,
    data_id            varchar(255)                           not null,
    group_id           varchar(128)                           not null,
    app_name           varchar(128) default NULL::character varying,
    content            text                                   not null,
    md5                varchar(32)  default NULL::character varying,
    gmt_create         timestamp    default CURRENT_TIMESTAMP not null,
    gmt_modified       timestamp    default CURRENT_TIMESTAMP not null,
    src_user           text,
    src_ip             varchar(50)  default NULL::character varying,
    op_type            char(10)     default NULL::bpchar,
    tenant_id          varchar(128) default ''::character varying,
    encrypted_data_key text
);

create index if not exists idx_gmt_create
    on public.his_config_info (gmt_create);

create index if not exists idx_gmt_modified
    on public.his_config_info (gmt_modified);

create index if not exists idx_did
    on public.his_config_info (data_id);

create table if not exists public.tenant_capacity
(
    id                bigserial
        primary key,
    tenant_id         varchar(128) default ''::character varying not null
        constraint uk_tenant_id
            unique,
    quota             integer      default 0                     not null,
    usage             integer      default 0                     not null,
    max_size          integer      default 0                     not null,
    max_aggr_count    integer      default 0                     not null,
    max_aggr_size     integer      default 0                     not null,
    max_history_count integer      default 0                     not null,
    gmt_create        timestamp    default CURRENT_TIMESTAMP     not null,
    gmt_modified      timestamp    default CURRENT_TIMESTAMP     not null
);

comment on table public.tenant_capacity is '租户容量信息表';

comment on column public.tenant_capacity.id is '主键id';

comment on column public.tenant_capacity.quota is '配额，0表示使用默认值';

comment on column public.tenant_capacity.usage is '使用量';

comment on column public.tenant_capacity.max_size is '单个配置大小上限，单位为字节，0表示使用默认值';

comment on column public.tenant_capacity.max_aggr_count is '聚合子配置最大个数，，0表示使用默认值';

comment on column public.tenant_capacity.max_aggr_size is '单个聚合数据的子配置大小上限，单位为字节，0表示使用默认值';

comment on column public.tenant_capacity.max_history_count is '最大变更历史数量';

comment on column public.tenant_capacity.gmt_create is '创建时间';

comment on column public.tenant_capacity.gmt_modified is '修改时间';

create table if not exists public.tenant_info
(
    id            bigserial
        primary key,
    kp            varchar(128) not null,
    tenant_id     varchar(128) default ''::character varying,
    tenant_name   varchar(128) default ''::character varying,
    tenant_desc   varchar(256) default NULL::character varying,
    create_source varchar(32)  default NULL::character varying,
    gmt_create    bigint       not null,
    gmt_modified  bigint       not null,
    constraint uk_tenant_info_kptenantid
        unique (kp, tenant_id)
);

comment on column public.tenant_info.id is '主键id';

comment on column public.tenant_info.gmt_create is '创建时间';

comment on column public.tenant_info.gmt_modified is '修改时间';

create index if not exists idx_tenant_info_tenant_id
    on public.tenant_info (tenant_id);

create table if not exists public.users
(
    username varchar(50)  not null
        primary key,
    password varchar(500) not null,
    enabled  boolean      not null
);

create table if not exists public.roles
(
    username varchar(50) not null,
    role     varchar(50) not null
);

create index if not exists idx_user_role
    on public.roles (username, role);

create table if not exists public.permissions
(
    role     varchar(50)  not null,
    resource varchar(255) not null,
    action   varchar(8)   not null
);

create index if not exists uk_role_permission
    on public.permissions (role, resource, action);

INSERT INTO users (username, password, enabled)
VALUES ('nacos', '$2a$10$EuWPZHzz32dJN7jexM34MOeYirDdFAZm2kuWj7VEOJhhZkDrxfvUu', TRUE);

INSERT INTO roles (username, role)
VALUES ('nacos', 'ROLE_ADMIN');