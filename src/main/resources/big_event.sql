-- 创建数据库
create database if not exists big_event;

-- 使用数据库
use big_event;

-- 用户表
create table user (
    id          int unsigned primary key auto_increment comment 'ID',
    username    varchar(20)  not null unique comment '用户名',
    password    varchar(32)  comment '密码',
    nickname    varchar(10)  default '' comment '昵称',
    email       varchar(128) default '' comment '邮箱',
    user_pic    varchar(128) default '' comment '头像',
    create_time datetime not null comment '创建时间',
    update_time datetime not null comment '修改时间'
) comment '用户表';

-- 分类表
create table category (
    id              int unsigned primary key auto_increment comment 'ID',
    category_name   varchar(32) not null comment '分类名称',
    category_alias  varchar(32) not null comment '分类别名',
    create_user     int unsigned not null comment '创建人ID',
    create_time     datetime not null comment '创建时间',
    update_time     datetime not null comment '修改时间',
    constraint fk_category_user foreign key (create_user) references user(id)
) comment '分类表';

-- 文章表
create table article (
    id          int unsigned primary key auto_increment comment 'ID',
    title       varchar(30)   not null comment '文章标题',
    content     varchar(10000) not null comment '文章内容',
    cover_img   varchar(128)  not null comment '文章封面',
    state       varchar(3)    default '草稿' comment '文章状态: 已发布/草稿',
    platform    varchar(20)   default 'bilibili' comment '来源平台: bilibili/zhihu/weibo/toutiao',
    topic_id    bigint unsigned comment '关联话题ID',
    category_id int unsigned  comment '文章分类ID',
    create_user int unsigned  not null comment '创建人ID',
    create_time datetime      not null comment '创建时间',
    update_time datetime      not null comment '修改时间',
    constraint fk_article_category foreign key (category_id) references category(id),
    constraint fk_article_user     foreign key (create_user)  references user(id),
    index idx_article_platform (platform),
    index idx_article_topic    (topic_id)
) comment '文章表';

-- ==================== 撰稿人AI 模块 ====================

-- B站分区 → 平台一级分区映射表
create table partition_mapping (
    id            int unsigned primary key auto_increment comment 'ID',
    tname         varchar(50) not null unique comment 'B站原始分区名（如极客DIY、数码等）',
    partition_tag varchar(20) not null comment '一级分区: 科技/游戏/生活/娱乐/知识/影视/体育/其他',
    created_at    datetime    not null default current_timestamp comment '创建时间'
) comment 'B站分区→平台分区映射表';

-- 初始分区映射数据
insert into partition_mapping (tname, partition_tag) values
    ('极客DIY',   '科技'),  ('软件应用', '科技'),  ('科工机械', '科技'),
    ('数码',      '科技'),  ('计算机技术','科技'),  ('编程开发','科技'),
    ('手机平板',  '科技'),  ('人工智能','科技'),
    ('单机游戏',  '游戏'),  ('手机游戏', '游戏'),  ('电子竞技', '游戏'),
    ('网络游戏',  '游戏'),  ('桌游棋牌', '游戏'),
    ('日常',      '生活'),  ('美食',     '生活'),  ('家居房产','生活'),
    ('动物圈',    '生活'),  ('运动',     '体育'),  ('健身','体育'),
    ('足球',      '体育'),  ('篮球',     '体育'),
    ('搞笑',      '娱乐'),  ('明星',     '娱乐'),  ('综艺','娱乐'),
    ('音乐综合',  '娱乐'),  ('舞蹈',     '娱乐'),
    ('科学科普',  '知识'),  ('人文历史', '知识'),  ('设计','知识'),
    ('社科·法律·心理','知识'), ('演讲·公开课','知识'),
    ('影视杂谈',  '影视'),  ('影视剪辑', '影视'),  ('短片','影视'),
    ('国产动画',  '动漫'),  ('MAD·AMV',  '动漫'),  ('综合动漫','动漫'),
    ('汽车',      '科技'),  ('摩托车',   '科技'),
    ('时尚',      '娱乐'),  ('美妆',     '娱乐'),
    ('资讯',      '知识'),  ('纪录片',   '知识');

-- 热点快照表（每次抓取B站热榜的完整记录，B站模块专用）
create table hot_snapshot (
    id              bigint unsigned primary key auto_increment comment 'ID',
    snapshot_time   datetime     not null comment '抓取时间（同一批次共享相同时间）',
    bvid            varchar(20)  not null comment 'B站视频BV号',
    title           varchar(200) not null comment '视频标题',
    url             varchar(500) comment '视频链接',
    cover_url       varchar(500) comment '封面图URL',
    author          varchar(100) comment 'UP主名称',
    tname           varchar(50)  comment 'B站原始分区名',
    partition_tag   varchar(20)  comment '映射后的一级分区',
    pubdate         datetime     comment '视频发布时间（来自B站extra.pubdate）',
    description     text         comment '视频简介（来自B站extra.desc）',
    `rank`          int          comment '当前快照中的排名',
    view_count      bigint       comment '播放量',
    like_count      bigint       comment '点赞数',
    coin_count      bigint       comment '投币数',
    favorite_count  bigint       comment '收藏数',
    share_count     bigint       comment '分享数',
    danmaku_count   bigint       comment '弹幕数',
    reply_count     bigint       comment '评论数',
    hot_score       decimal(10,2) comment '综合热度评分（由评分引擎计算）',
    created_at      datetime     default current_timestamp comment '记录创建时间',
    index idx_snapshot_time (snapshot_time),
    index idx_bvid (bvid),
    index idx_partition (partition_tag),
    unique key uk_snapshot_bvid (bvid)
) comment '热点快照表-B站热榜历史记录';

-- ==================== 平台化架构 V2 ====================

-- 原始信号表（多平台信号统一采集层，替代 hot_snapshot 的平台局限性）
create table raw_signal (
    id            bigint unsigned primary key auto_increment comment 'ID',
    source        varchar(20)  not null comment '平台来源: bilibili/zhihu/weibo/toutiao',
    source_id     varchar(100) not null comment '平台内唯一ID (bvid/热搜ID/问题ID)',
    title         varchar(500) not null comment '原始标题',
    url           varchar(500) comment '原文链接',
    author        varchar(100) comment '作者/UP主',
    cover_url     varchar(500) comment '封面图URL',
    category      varchar(50)  comment '原始分类名',
    partition_tag varchar(20)  comment '归一化后的一级分区',
    `rank`        int          comment '排名',
    raw_metrics   json         comment '平台原始指标 (播放量/点赞/搜索指数等)',
    norm_score    decimal(6,2) comment '平台内归一化热度分(0-100)',
    fetched_at    datetime     not null comment '采集时间',
    created_at    datetime     default current_timestamp comment '记录创建时间',
    index idx_raw_source (source),
    index idx_raw_fetched (fetched_at),
    index idx_raw_partition (partition_tag),
    unique key uk_raw_signal (source, source_id, fetched_at)
) comment '原始信号表-多平台热点信号统一采集层';

-- 话题表（核心新实体 —— 跨平台话题归一化）
create table topic (
    id            bigint unsigned primary key auto_increment comment 'ID',
    title         varchar(200) not null comment '标准化话题名称',
    aliases       json         comment '别名列表',
    keywords      json         comment '关键词',
    first_seen_at datetime     comment '首次出现时间',
    last_seen_at  datetime     comment '最近出现时间',
    status        varchar(20)  default 'active' comment '状态: active/archived/merged',
    merged_into   bigint unsigned comment '合并到哪个话题ID',
    created_at    datetime     default current_timestamp comment '创建时间',
    index idx_topic_status (status),
    index idx_topic_last_seen (last_seen_at)
) comment '话题表-跨平台话题归一化实体';

-- 信号-话题关联表
create table signal_topic_rel (
    signal_id     bigint unsigned not null comment '信号ID',
    topic_id      bigint unsigned not null comment '话题ID',
    confidence    decimal(4,3) comment '归属置信度(0-1)',
    primary key (signal_id, topic_id)
) comment '信号话题关联表';

-- AI处理记录表（追踪每个热点是否已生成文章，防止重复）
create table ai_process_record (
    id              int unsigned primary key auto_increment comment 'ID',
    bvid            varchar(20)  comment 'B站视频BV号（向后兼容，新代码使用 source + source_id）',
    source          varchar(20)  not null default 'bilibili' comment '平台来源',
    source_id       varchar(100) not null comment '平台内唯一ID',
    article_id      int unsigned comment '生成的文章ID，关联article表',
    process_time    datetime     not null comment '处理时间',
    status          varchar(20)  not null default 'generated' comment '状态: generated/published/rejected',
    ai_model        varchar(50)  comment '使用的AI模型',
    hot_score       decimal(10,2) comment '处理时的热度评分',
    created_at      datetime     default current_timestamp comment '创建时间',
    index idx_article_id (article_id),
    unique key uk_ai_source (source, source_id)
) comment 'AI处理记录表-追踪每个热点是否已生成文章';

-- ==================== V2.1 迁移 ====================
-- 为已有 hot_snapshot 表新增 pubdate 和 description 字段
-- 注意：如果列已存在会报错（Duplicate column），跳过即可
ALTER TABLE hot_snapshot ADD COLUMN pubdate     datetime comment '视频发布时间（来自B站extra.pubdate）' AFTER partition_tag;
ALTER TABLE hot_snapshot ADD COLUMN description text     comment '视频简介（来自B站extra.desc）'      AFTER pubdate;

-- ==================== V2.2 知识库 ====================

-- 知识库文章表（用于AI撰稿时提供风格参考和事实素材，减少AI味）
create table knowledge_article (
    id          bigint unsigned primary key auto_increment comment 'ID',
    title       varchar(200)   not null comment '文章标题',
    content     mediumtext     not null comment '全文（Markdown）',
    excerpt     varchar(500)   default null comment '摘要/核心观点',
    category    varchar(50)    default null comment '分类（科技/社会/娱乐/教育...）',
    tags        varchar(500)   default null comment '逗号分隔标签',
    author      varchar(100)   default null comment '原作者名',
    source_url  varchar(500)   default null comment '来源链接',
    quality     tinyint        default 3 comment '质量评级 1-5',
    word_count  int            default 0 comment '字数',
    status      tinyint        default 1 comment '1=启用 0=停用',
    created_at  datetime       default current_timestamp comment '创建时间',
    updated_at  datetime       default current_timestamp on update current_timestamp comment '修改时间',
    index idx_category (category),
    index idx_status (status),
    index idx_quality (quality)
) comment '知识库文章表-用于AI撰稿风格参考';

-- ==================== V2.3 hot_snapshot 去重改造 ====================

-- 将 hot_snapshot 唯一键从 (snapshot_time, bvid) 改为 (bvid)，实现 upsert
-- 注意：执行前会清空已有数据（因为历史快照不再需要，趋势分析已迁移到 raw_signal）
ALTER TABLE hot_snapshot DROP INDEX uk_snapshot_bvid;
ALTER TABLE hot_snapshot ADD UNIQUE KEY uk_snapshot_bvid (bvid);
