create table chat_session
(
    id         bigint auto_increment comment '会话ID'
        primary key,
    user_id    bigint                             not null comment '所属用户ID',
    memory_id  varchar(100)                       not null comment '前端传入的memoryId，也是AI会话记忆ID',
    title      varchar(100)                       not null comment '会话标题，默认取第一条用户消息',
    created_at datetime default CURRENT_TIMESTAMP null comment '创建时间',
    updated_at datetime default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP comment '更新时间',
    unique key uk_user_memory (user_id, memory_id)
)
    comment '聊天会话表';

create table chat_message
(
    id         bigint auto_increment comment '消息ID'
        primary key,
    user_id    bigint                             not null comment '所属用户ID',
    session_id bigint                             not null comment '所属会话ID',
    role       varchar(20)                        not null comment '消息角色：user=用户，assistant=AI',
    content    longtext                           not null comment '消息内容',
    created_at datetime default CURRENT_TIMESTAMP null comment '创建时间',
    index idx_session_user (session_id, user_id)
)
    comment '聊天消息表';
