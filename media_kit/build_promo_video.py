#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
基金实时估值系统 - B站 1080P 高清宣传渲染视频全自动构建脚本 (升级版)
针对用户要求深度优化:
1. 增加【自选基金页】核心介绍，按产品使用顺序放在前面;
2. 第一帧黄金钩子极度压缩至 3.2 秒，开篇紧凑抓人;
3. 完整保留并精细化呈现【对标支付宝4大核心指标】;
4. 每一帧界面全面对标 Apple / Linear / Ant Design Pro 高级现代设计，
   包含 macOS 红黄绿窗口控件、波形走势脉冲、等宽金融数字、动态光晕、激光扫描线与高对比度悬浮字幕。
"""

import os
import sys
import math
import struct
import wave
import asyncio
import subprocess
from pathlib import Path
from PIL import Image, ImageDraw, ImageFont

if sys.platform.startswith('win'):
    try:
        sys.stdout.reconfigure(encoding='utf-8')
        sys.stderr.reconfigure(encoding='utf-8')
    except Exception:
        pass

# 目录配置
SCRIPT_DIR = Path(__file__).resolve().parent
OUTPUT_DIR = SCRIPT_DIR
TEMP_DIR = OUTPUT_DIR / "temp"
TEMP_DIR.mkdir(parents=True, exist_ok=True)

FINAL_VIDEO_PATH = OUTPUT_DIR / "fund_valuation_promo.mp4"
PROMO_KIT_PATH = OUTPUT_DIR / "BILIBILI_PROMO_KIT.md"

# 基础规格
WIDTH, HEIGHT = 1920, 1080
FPS = 30
SAMPLE_RATE = 44100

# 字体配置 (优先 Windows 系统微软雅黑粗体)
FONT_PATHS = [
    Path(r"C:\Windows\Fonts\msyhbd.ttc"),
    Path(r"C:\Windows\Fonts\msyh.ttc"),
    Path(r"C:\Windows\Fonts\simhei.ttf"),
]

def get_font(size: int, bold: bool = True):
    for fp in FONT_PATHS:
        if fp.exists():
            try:
                return ImageFont.truetype(str(fp), size)
            except Exception:
                pass
    return ImageFont.load_default()

FONT_TITLE = get_font(48, bold=True)
FONT_SUBTITLE = get_font(24, bold=False)
FONT_CARD_TITLE = get_font(30, bold=True)
FONT_BIG_NUM = get_font(42, bold=True)
FONT_MEDIUM_NUM = get_font(28, bold=True)
FONT_BODY = get_font(22, bold=False)
FONT_CAPTION = get_font(16, bold=False)
FONT_SUBTITLE_BAR = get_font(24, bold=True)

# 7 个精心打磨的宣传分镜 (顺序更符合使用流，单句 3.0~4.5 秒，总长约 29~33 秒)
SCENES = [
    {
        "id": 1,
        "tag": "秒级测算 · 拒绝盲盒",
        "title": "还在盲猜今天基金赚了多少钱？",
        "subtitle": "基金实时估值系统 · 盘中持仓动态收益测算神器",
        "tts_text": "还在盲猜今天亏了几百？自研实时估值神器来了！",
        "subtitle_text": "还在盲猜今天亏了几百？自研实时估值神器来了！",
    },
    {
        "id": 2,
        "tag": "自选大盘 · 秒级高频追踪",
        "title": "自选基金大盘 · 毫秒级估值脉搏",
        "subtitle": "支持代码批量秒级导入 · 拖拽自由排序 · 多类型穿透实时估值",
        "tts_text": "首页自选大盘，支持代码批量极速添加与自由拖拽排序，盘中秒级追踪全市场标的估算涨跌！",
        "subtitle_text": "首页自选大盘，支持批量添加与拖拽排序，盘中秒级追踪标的估值！",
    },
    {
        "id": 3,
        "tag": "对标支付宝持仓 · 零门槛记账",
        "title": "对标支付宝持仓 · 4大指标智能联动",
        "subtitle": "持有金额、昨日收益、持有收益、收益率 · 双向毫秒级智能推算",
        "tts_text": "持仓录入全面对标支付宝4大核心指标，金额与收益率双向联动，一键同步无需繁琐记账！",
        "subtitle_text": "持仓录入对标支付宝4大核心指标，金额与收益率双向智能联动！",
    },
    {
        "id": 4,
        "tag": "钱包盈亏 · 实时跳动",
        "title": "今日收益实时看板 · 每一分钱清晰可见",
        "subtitle": "今日预估收益 · 总资产市值 · 累计盈亏每分钟自动计算",
        "tts_text": "进入持仓看板，结合盘中实时估值，每分钟自动推算今日到手收益，大盘涨跌尽在掌握！",
        "subtitle_text": "进入持仓看板，结合盘中实时估值，每分钟自动推算今日到手收益！",
    },
    {
        "id": 5,
        "tag": "双核硬核穿透 · 拒绝失真",
        "title": "双核硬核穿透引擎 · 拒绝滞后与失真",
        "subtitle": "股票前十大重仓秒级穿透 + 纯债十年国债利率驱动模型",
        "tts_text": "双核硬核穿透引擎，股票秒级穿透前十大重仓，纯债依托国债利率与久期模型精准测算！",
        "subtitle_text": "股票秒级穿透前十大重仓，纯债依托国债利率与久期模型精准测算！",
    },
    {
        "id": 6,
        "tag": "全端响应式 · 自动化后台",
        "title": "全端响应式适配 · 企业级控制大盘",
        "subtitle": "移动端吸顶卡片流 + PC专业表格 · 用户免审开关与收款码动态配置",
        "tts_text": "手机与PC端深度响应式适配，配套企业级后台，新用户免审一键切换，运营调度全自动化！",
        "subtitle_text": "手机与电脑多端自适应，配套管理后台，新用户免审一键切换！",
    },
    {
        "id": 7,
        "tag": "新用户福利 · 立即免费体验",
        "title": "新用户注册 · 即刻赠送 7 天 VIP 体验",
        "subtitle": "持仓今日收益估算特权全解锁 · 体验与源码见置顶评论",
        "tts_text": "新用户注册即赠7天全功能VIP体验！体验地址见置顶评论，喜欢请务必一键三连支持一下！",
        "subtitle_text": "新用户注册即赠7天VIP体验！体验地址见置顶评论，求三连！",
    },
]

# -------------------------------------------------------------
# 1. edge-tts 语音合成 (带指数退避重试)
# -------------------------------------------------------------
async def generate_speech_file(text: str, output_file: Path, max_retries: int = 3):
    import edge_tts
    for attempt in range(max_retries):
        try:
            # 语速 +22%，活力干脆，毫不拖泥带水
            comm = edge_tts.Communicate(text, "zh-CN-YunxiNeural", rate="+22%")
            await comm.save(str(output_file))
            if output_file.exists() and output_file.stat().st_size > 1000:
                return
        except Exception as e:
            if attempt == max_retries - 1:
                raise e
            await asyncio.sleep(1.2 * (attempt + 1))

def get_audio_duration(file_path: Path) -> float:
    cmd = [
        "ffprobe", "-v", "error",
        "-show_entries", "format=duration",
        "-of", "default=noprint_wrappers=1:nokey=1",
        str(file_path)
    ]
    res = subprocess.run(cmd, capture_output=True, text=True, check=True)
    return float(res.stdout.strip())

# -------------------------------------------------------------
# 2. 生成悦耳科技感 Lo-Fi 背景音乐 (44.1kHz 16-bit 立体声 WAV)
# -------------------------------------------------------------
def generate_lofi_bgm(output_wav: Path, total_seconds: float):
    print(f"🎵 正在生成高品质轻科技 Lo-Fi 背景音乐 ({total_seconds:.1f} 秒)...")
    total_samples = int(total_seconds * SAMPLE_RATE)
    chords = [
        [261.63, 329.63, 392.00, 493.88],  # Cmaj7
        [220.00, 261.63, 329.63, 392.00],  # Am7
        [174.61, 220.00, 261.63, 329.63],  # Fmaj7
        [196.00, 246.94, 293.66, 349.23],  # G7
    ]
    chord_len = 2.5

    with wave.open(str(output_wav), "w") as wav:
        wav.setnchannels(2)
        wav.setsampwidth(2)
        wav.setframerate(SAMPLE_RATE)
        frames = bytearray()
        for i in range(total_samples):
            t = i / SAMPLE_RATE
            chord_idx = int((t / chord_len) % len(chords))
            cur_chord = chords[chord_idx]
            
            beat_phase = (t * 85 / 60) % 1.0
            envelope = math.exp(-beat_phase * 2.8) * 0.75 + 0.25
            
            chord_sample = 0.0
            for freq in cur_chord:
                chord_sample += math.sin(2 * math.pi * freq * t) * 0.11
                chord_sample += math.sin(4 * math.pi * freq * t) * 0.025
            chord_sample *= envelope

            root_freq = cur_chord[0] / 2.0
            bass_sample = math.sin(2 * math.pi * root_freq * t) * 0.14
            
            drum_phase = (t * 170 / 60) % 1.0
            kick = math.sin(2 * math.pi * 50 * (1 - drum_phase * 0.8) * t) * math.exp(-drum_phase * 16) * 0.16

            sample_val = chord_sample + bass_sample + kick
            sample_val = max(-0.95, min(0.95, sample_val * 0.35))
            int_val = int(sample_val * 32767.0)
            frames.extend(struct.pack("<hh", int_val, int_val))
            
        wav.writeframes(frames)
    print("✅ 背景音乐合成完毕！")

# -------------------------------------------------------------
# 3. 现代化视觉组件与动效渲染
# -------------------------------------------------------------
def draw_gradient_background(draw: ImageDraw.ImageDraw, frame_idx: int):
    # 浅灰蓝到暖白极简渐变
    for y in range(0, HEIGHT, 2):
        ratio = y / HEIGHT
        r = int(248 * (1 - ratio) + 238 * ratio)
        g = int(250 * (1 - ratio) + 243 * ratio)
        b = int(252 * (1 - ratio) + 248 * ratio)
        draw.line([(0, y), (WIDTH, y)], fill=(r, g, b), width=2)

    # 科技微点阵网格
    grid_gap = 36
    for x in range(20, WIDTH, grid_gap):
        for y in range(20, HEIGHT, grid_gap):
            draw.point((x, y), fill=(203, 213, 225, 80))

    # 动态彩色光环 (左上淡蓝、右上淡紫、右下淡金)
    wave_offset = math.sin(frame_idx * 0.1) * 18
    draw.ellipse([(-80, -80 + wave_offset), (460, 460 + wave_offset)], fill=(224, 242, 254, 45))
    draw.ellipse([(WIDTH - 460, -80 - wave_offset), (WIDTH + 80, 460 - wave_offset)], fill=(245, 243, 255, 40))
    draw.ellipse([(WIDTH - 500, HEIGHT - 500 + wave_offset), (WIDTH + 100, HEIGHT + 100 + wave_offset)], fill=(254, 243, 199, 45))

def draw_main_window(draw: ImageDraw.ImageDraw, card_box, tag_text: str):
    # 外层投影模拟
    draw.rounded_rectangle([card_box[0]-4, card_box[1]-4, card_box[2]+4, card_box[3]+4], radius=22, fill=(226, 232, 240, 90))
    draw.rounded_rectangle([card_box[0]-2, card_box[1]-2, card_box[2]+2, card_box[3]+2], radius=20, fill=(241, 245, 249, 140))
    # 主卡片白底
    draw.rounded_rectangle(card_box, radius=18, fill=(255, 255, 255), outline=(226, 232, 240), width=2)

    # macOS 经典红黄绿交通灯微控件 (左上角)
    tx, ty = card_box[0] + 28, card_box[1] + 24
    draw.ellipse([tx, ty, tx + 14, ty + 14], fill=(239, 68, 68))      # 红
    draw.ellipse([tx + 22, ty, tx + 36, ty + 14], fill=(245, 158, 11)) # 黄
    draw.ellipse([tx + 44, ty, tx + 58, ty + 14], fill=(34, 197, 94))  # 绿

    # 顶栏居中 Logo 与系统名
    logo_cx = card_box[0] + 120
    draw.text((logo_cx, ty - 2), "基金实时估值系统 · Fund Valuation", fill=(15, 23, 42), font=FONT_CAPTION)

    # 右侧特色标签 Pill
    tag_w = 310
    tag_box = [card_box[2] - 30 - tag_w, card_box[1] + 14, card_box[2] - 30, card_box[1] + 46]
    draw.rounded_rectangle(tag_box, radius=16, fill=(239, 246, 255), outline=(191, 219, 254), width=1)
    draw.ellipse([tag_box[0] + 14, tag_box[1] + 12, tag_box[0] + 22, tag_box[1] + 20], fill=(22, 163, 74))
    draw.text((tag_box[0] + 30, tag_box[1] + 7), tag_text, fill=(30, 64, 175), font=FONT_CAPTION)

    # 分割线
    draw.line([(card_box[0], card_box[1] + 58), (card_box[2], card_box[1] + 58)], fill=(241, 245, 249), width=1)

def draw_floating_subtitle_bar(draw: ImageDraw.ImageDraw, text: str):
    # 底部高对比度纯净悬浮字幕条
    sub_w = 1440
    sub_h = 56
    sub_x = (WIDTH - sub_w) // 2
    sub_y = 868
    draw.rounded_rectangle([sub_x, sub_y, sub_x + sub_w, sub_y + sub_h], radius=28, fill=(15, 23, 42), outline=(51, 65, 85), width=1)
    bbox = draw.textbbox((0, 0), text, font=FONT_SUBTITLE_BAR)
    tw = bbox[2] - bbox[0]
    draw.text((sub_x + (sub_w - tw) // 2, sub_y + 13), text, fill=(255, 255, 255), font=FONT_SUBTITLE_BAR)

def draw_laser_scan_line(draw: ImageDraw.ImageDraw, card_box, frame_idx: int):
    card_h = card_box[3] - card_box[1] - 80
    scan_y = card_box[1] + 70 + int((math.sin(frame_idx * 0.12) * 0.5 + 0.5) * card_h)
    draw.line([(card_box[0] + 24, scan_y), (card_box[2] - 24, scan_y)], fill=(37, 99, 235, 60), width=2)

# -------------------------------------------------------------
# 4. 各场景专属内容高精度排版
# -------------------------------------------------------------
def render_scene_content(draw: ImageDraw.ImageDraw, scene: dict, card_box: list, frame_idx: int):
    sid = scene["id"]
    cx1, cy1, cx2, cy2 = card_box
    content_y = cy1 + 80

    # 卡片内顶栏标题区 (统一规范: 48px 粗黑标题 + 24px 浅蓝副标)
    draw.text((cx1 + 44, content_y), scene["title"], fill=(15, 23, 42), font=FONT_TITLE)
    draw.text((cx1 + 46, content_y + 64), scene["subtitle"], fill=(100, 116, 139), font=FONT_SUBTITLE)
    draw.line([(cx1 + 44, content_y + 104), (cx2 - 44, content_y + 104)], fill=(241, 245, 249), width=2)
    
    body_y = content_y + 124

    if sid == 1:
        # 分镜1: 震撼视觉开场 (超清大图标 + 痛点直击 + 极速转折)
        icon_cx, icon_cy = cx1 + 220, body_y + 200
        pulse = math.sin(frame_idx * 0.16) * 10
        draw.ellipse([icon_cx - 140 - pulse, icon_cy - 140 - pulse, icon_cx + 140 + pulse, icon_cy + 140 + pulse], fill=(239, 246, 255))
        draw.ellipse([icon_cx - 100, icon_cy - 100, icon_cx + 100, icon_cy + 100], fill=(219, 234, 254), outline=(147, 197, 253), width=2)
        # 矢量柱状动效
        draw.rounded_rectangle([icon_cx - 55, icon_cy + 10, icon_cx - 30, icon_cy + 65], radius=6, fill=(37, 99, 235))
        draw.rounded_rectangle([icon_cx - 15, icon_cy - 25, icon_cx + 10, icon_cy + 65], radius=6, fill=(16, 185, 129))
        draw.rounded_rectangle([icon_cx + 25, icon_cy - 60, icon_cx + 50, icon_cy + 65], radius=6, fill=(239, 68, 68))

        px = cx1 + 440
        # 痛点 vs 突破双对比卡片
        draw.rounded_rectangle([px, body_y + 10, px + 540, body_y + 185], radius=16, fill=(254, 242, 242), outline=(254, 202, 202), width=1)
        draw.text((px + 28, body_y + 32), "传统理财 App 痛点", fill=(220, 38, 38), font=FONT_CARD_TITLE)
        draw.text((px + 28, body_y + 82), "• 只有百分比，今天到底赚了/亏了几百几千？", fill=(153, 27, 27), font=FONT_BODY)
        draw.text((px + 28, body_y + 120), "• 债券基金持仓不公开，盘中全靠盲猜！", fill=(185, 28, 28), font=FONT_BODY)

        draw.rounded_rectangle([px + 570, body_y + 10, px + 1110, body_y + 185], radius=16, fill=(240, 253, 244), outline=(187, 247, 208), width=1)
        draw.text((px + 598, body_y + 32), "自研实时估值方案", fill=(22, 163, 74), font=FONT_CARD_TITLE)
        draw.text((px + 598, body_y + 82), "• 对标支付宝4指标，直接算清今日到手金额！", fill=(22, 101, 52), font=FONT_BODY)
        draw.text((px + 598, body_y + 120), "• 股票穿透重仓 + 债券国债久期利率模型！", fill=(22, 101, 52), font=FONT_BODY)

        # 核心亮点流
        feats = ["盘中每分钟实时测算", "股票前十重仓秒级穿透", "10年期国债利率驱动", "多端丝滑自适应", "新用户赠送7天VIP"]
        fx = cx1 + 440
        for idx, feat in enumerate(feats):
            fb = [fx + idx * 224, body_y + 215, fx + idx * 224 + 210, body_y + 275]
            draw.rounded_rectangle(fb, radius=12, fill=(248, 250, 252), outline=(226, 232, 240), width=1)
            draw.text((fb[0] + 16, fb[1] + 18), feat, fill=(51, 65, 85), font=FONT_CAPTION)

    elif sid == 2:
        # 分镜2: 自选基金大盘 (代码批量添加 + 拖拽排序 + 实时估算与胶囊徽章)
        # 操作工具栏模拟
        tb = [cx1 + 44, body_y + 8, cx2 - 44, body_y + 60]
        draw.rounded_rectangle(tb, radius=12, fill=(248, 250, 252), outline=(226, 232, 240), width=1)
        draw.text((tb[0] + 20, tb[1] + 13), "自选基金库 (共 28 只)", fill=(15, 23, 42), font=FONT_BODY)
        
        # 按钮模拟
        draw.rounded_rectangle([tb[2] - 340, tb[1] + 8, tb[2] - 210, tb[3] - 8], radius=8, fill=(37, 99, 235))
        draw.text((tb[2] - 324, tb[1] + 13), "+ 批量添加", fill=(255, 255, 255), font=FONT_CAPTION)
        draw.rounded_rectangle([tb[2] - 190, tb[1] + 8, tb[2] - 90, tb[3] - 8], radius=8, fill=(255, 255, 255), outline=(203, 213, 225))
        draw.text((tb[2] - 175, tb[1] + 13), "拖拽排序", fill=(71, 85, 105), font=FONT_CAPTION)
        draw.rounded_rectangle([tb[2] - 70, tb[1] + 8, tb[2] - 15, tb[3] - 8], radius=8, fill=(255, 255, 255), outline=(203, 213, 225))
        draw.text((tb[2] - 56, tb[1] + 13), "刷新", fill=(71, 85, 105), font=FONT_CAPTION)

        # 3行自选基金列表模拟 (包含类型Tag、昨净值、实时估算净值、红绿胶囊徽章)
        items = [
            ("易方达消费行业股票", "110022", "主动股票", "2.5500", "+2.00%", "2.5000", "09-29", True),
            ("华夏国证半导体芯片ETF联接", "008888", "指数增强", "1.1280", "+1.15%", "1.1152", "09-29", True),
            ("富国产业债债券A", "100058", "债券型-长债", "1.2385", "-0.12%", "1.2400", "09-29", False),
        ]
        for idx, (fname, fcode, ftype, estnav, estpct, prevnav, navdate, isup) in enumerate(items):
            iy = body_y + 75 + idx * 72
            ibox = [cx1 + 44, iy, cx2 - 44, iy + 62]
            draw.rounded_rectangle(ibox, radius=10, fill=(255, 255, 255), outline=(226, 232, 240), width=1)
            # 拖拽手柄图标 (6个圆点)
            hx, hy = ibox[0] + 16, iy + 22
            for dx in (0, 6):
                for dy in (0, 7, 14):
                    draw.ellipse([hx + dx, hy + dy, hx + dx + 3, hy + dy + 3], fill=(148, 163, 184))
            
            # 名称与代码
            draw.text((ibox[0] + 45, iy + 10), fname, fill=(15, 23, 42), font=FONT_BODY)
            draw.text((ibox[0] + 45, iy + 36), f"{fcode} · 昨净 {prevnav} ({navdate})", fill=(100, 116, 139), font=FONT_CAPTION)
            
            # 类型Tag
            tag_color = (224, 231, 255) if "指数" in ftype else (254, 226, 226) if "主动" in ftype else (220, 252, 231)
            text_color = (67, 56, 202) if "指数" in ftype else (185, 28, 28) if "主动" in ftype else (21, 128, 61)
            tbox = [ibox[0] + 370, iy + 14, ibox[0] + 470, iy + 42]
            draw.rounded_rectangle(tbox, radius=6, fill=tag_color)
            draw.text((tbox[0] + 12, tbox[1] + 5), ftype[:4], fill=text_color, font=FONT_CAPTION)

            # 估算净值 (大等宽)
            draw.text((ibox[2] - 280, iy + 14), estnav, fill=(220, 38, 38) if isup else (22, 163, 74), font=FONT_MEDIUM_NUM)
            
            # 估算涨跌幅徽章
            bbox = [ibox[2] - 130, iy + 13, ibox[2] - 20, iy + 49]
            draw.rounded_rectangle(bbox, radius=6, fill=(254, 242, 242) if isup else (240, 253, 244), outline=(254, 202, 202) if isup else (187, 247, 208), width=1)
            draw.text((bbox[0] + 18, bbox[1] + 8), estpct, fill=(220, 38, 38) if isup else (22, 163, 74), font=FONT_BODY)

        # 底部优势横幅
        tip_box = [cx1 + 44, body_y + 300, cx2 - 44, body_y + 365]
        draw.rounded_rectangle(tip_box, radius=12, fill=(240, 249, 255), outline=(186, 230, 253), width=1)
        draw.text((tip_box[0] + 28, tip_box[1] + 18), "⚡ 批量输入多代码以逗号/换行分隔一次性加入自选；拖拽任意手柄即可自如调整个人看板优先顺序！", fill=(3, 105, 161), font=FONT_BODY)

    elif sid == 3:
        # 分镜3: 对标支付宝持仓 4 大指标智能联动
        draw.text((cx1 + 44, body_y + 10), "与支付宝「基金持有」界面数据 100% 结构化对齐：", fill=(51, 65, 85), font=FONT_BODY)
        col_w = 370
        fields = [
            ("持有金额 (资产规模)", "¥ 20,000.00", "当前持仓基准现值", (37, 99, 235), (239, 246, 255)),
            ("昨日收益 (官方结算)", "+¥ 50.00", "昨日账面实际已确认", (220, 38, 38), (254, 242, 242)),
            ("持有收益 (累计盈亏)", "+¥ 1,500.00", "建仓至今总盈亏金额", (220, 38, 38), (254, 242, 242)),
            ("持有收益率", "+8.11 %", "双向自动反算持仓成本", (16, 185, 129), (240, 253, 244)),
        ]
        for idx, (fname, fval, fdesc, tcol, bcol) in enumerate(fields):
            bx = cx1 + 44 + idx * (col_w + 34)
            bbox = [bx, body_y + 55, bx + col_w, body_y + 250]
            draw.rounded_rectangle(bbox, radius=16, fill=bcol, outline=(226, 232, 240), width=1)
            draw.text((bx + 24, bbox[1] + 22), fname, fill=(100, 116, 139), font=FONT_BODY)
            draw.text((bx + 24, bbox[1] + 72), fval, fill=tcol, font=FONT_BIG_NUM)
            draw.text((bx + 24, bbox[1] + 138), fdesc, fill=(71, 85, 105), font=FONT_CAPTION)

        banner = [cx1 + 44, body_y + 280, cx2 - 44, body_y + 355]
        draw.rounded_rectangle(banner, radius=14, fill=(248, 250, 252), outline=(226, 232, 240), width=1)
        draw.text((banner[0] + 30, banner[1] + 22), "⚡ 零心智负担：输入金额与收益，自动秒算成本与收益率；数据结构已为支付宝截图 OCR 自动录入做好准备！", fill=(30, 41, 59), font=FONT_BODY)

    elif sid == 4:
        # 分镜4: 持仓今日收益实时测算看板
        col_w = 370
        cards = [
            ("今日收益 (盘中实时)", "+¥ 386.50", "日内涨跌: +1.32%", (220, 38, 38), (254, 242, 242)),
            ("总市值 (动态现值)", "¥ 68,520.00", "基准金额: ¥68,133.50", (15, 23, 42), (255, 255, 255)),
            ("累计盈亏", "+¥ 4,280.00", "总回报率: +6.67%", (220, 38, 38), (255, 255, 255)),
            ("昨日收益 (官方)", "+¥ 120.00", "支付宝账面确认到账", (15, 23, 42), (255, 255, 255)),
        ]
        for idx, (cname, cval, csub, ccol, cbg) in enumerate(cards):
            bx = cx1 + 44 + idx * (col_w + 34)
            bbox = [bx, body_y + 8, bx + col_w, body_y + 168]
            draw.rounded_rectangle(bbox, radius=14, fill=cbg, outline=(226, 232, 240), width=1)
            draw.text((bx + 20, bbox[1] + 16), cname, fill=(100, 116, 139), font=FONT_CAPTION)
            draw.text((bx + 20, bbox[1] + 48), cval, fill=ccol, font=FONT_BIG_NUM)
            draw.text((bx + 20, bbox[1] + 114), csub, fill=(100, 116, 139), font=FONT_CAPTION)

        item_y = body_y + 185
        row1 = [cx1 + 44, item_y, cx2 - 44, item_y + 75]
        draw.rounded_rectangle(row1, radius=12, fill=(255, 255, 255), outline=(226, 232, 240), width=1)
        draw.text((row1[0] + 24, row1[1] + 16), "易方达消费行业股票 (110022)", fill=(15, 23, 42), font=FONT_BODY)
        draw.text((row1[0] + 24, row1[1] + 44), "持有: ¥20,000.00 · 昨日收益: +¥50.00 · 持有收益: +¥1,500.00", fill=(100, 116, 139), font=FONT_CAPTION)
        draw.text((row1[2] - 380, row1[1] + 22), "今日收益: +¥260.00", fill=(220, 38, 38), font=FONT_MEDIUM_NUM)
        draw.rounded_rectangle([row1[2] - 130, row1[1] + 20, row1[2] - 24, row1[1] + 55], radius=6, fill=(254, 242, 242), outline=(254, 202, 202), width=1)
        draw.text((row1[2] - 116, row1[1] + 24), "+1.30%", fill=(220, 38, 38), font=FONT_BODY)

        row2 = [cx1 + 44, item_y + 85, cx2 - 44, item_y + 160]
        draw.rounded_rectangle(row2, radius=12, fill=(255, 255, 255), outline=(226, 232, 240), width=1)
        draw.text((row2[0] + 24, row2[1] + 16), "华夏国证半导体芯片ETF联接 (008888)", fill=(15, 23, 42), font=FONT_BODY)
        draw.text((row2[0] + 24, row2[1] + 44), "持有: ¥15,000.00 · 昨日收益: -¥30.00 · 持有收益: +¥850.00", fill=(100, 116, 139), font=FONT_CAPTION)
        draw.text((row2[2] - 380, row2[1] + 22), "今日收益: +¥126.50", fill=(220, 38, 38), font=FONT_MEDIUM_NUM)
        draw.rounded_rectangle([row2[2] - 130, row2[1] + 20, row2[2] - 24, row2[1] + 55], radius=6, fill=(254, 242, 242), outline=(254, 202, 202), width=1)
        draw.text((row2[2] - 116, row2[1] + 24), "+0.84%", fill=(220, 38, 38), font=FONT_BODY)

    elif sid == 5:
        # 分镜5: 双核硬核穿透引擎 (股票重仓穿透 + 纯债久期利率模型)
        half_w = (cx2 - cx1 - 120) // 2
        left_box = [cx1 + 44, body_y + 10, cx1 + 44 + half_w, body_y + 360]
        draw.rounded_rectangle(left_box, radius=16, fill=(248, 250, 252), outline=(203, 213, 225), width=1)
        draw.text((left_box[0] + 26, left_box[1] + 22), "股票型/混合型 · 穿透前十大重仓", fill=(30, 41, 59), font=FONT_CARD_TITLE)
        draw.text((left_box[0] + 26, left_box[1] + 68), "直连主流交易所极速行情，按季度报告持仓权重逐只穿透加权：", fill=(100, 116, 139), font=FONT_CAPTION)
        
        stocks = [("贵州茅台 (600519)", "9.85%", "+1.65%"), ("宁德时代 (300750)", "8.42%", "+2.30%"), ("腾讯控股 (00700)", "7.10%", "-0.45%")]
        for sidx, (sname, sw, spct) in enumerate(stocks):
            sy = left_box[1] + 115 + sidx * 64
            srow = [left_box[0] + 24, sy, left_box[2] - 24, sy + 52]
            draw.rounded_rectangle(srow, radius=8, fill=(255, 255, 255), outline=(226, 232, 240), width=1)
            draw.text((srow[0] + 16, sy + 14), sname, fill=(15, 23, 42), font=FONT_BODY)
            draw.text((srow[0] + 360, sy + 14), f"权重 {sw}", fill=(100, 116, 139), font=FONT_CAPTION)
            is_up = "+" in spct
            draw.text((srow[2] - 110, sy + 14), spct, fill=(220, 38, 38) if is_up else (22, 163, 74), font=FONT_BODY)

        right_box = [cx1 + 44 + half_w + 24, body_y + 10, cx2 - 44, body_y + 360]
        draw.rounded_rectangle(right_box, radius=16, fill=(240, 249, 255), outline=(186, 230, 253), width=1)
        draw.text((right_box[0] + 26, right_box[1] + 22), "纯债基金 · 10年期国债利率驱动模型", fill=(3, 105, 161), font=FONT_CARD_TITLE)
        draw.text((right_box[0] + 26, right_box[1] + 68), "纯债不公开持仓秒级行情，系统采用久期与国债利率实时测算：", fill=(100, 116, 139), font=FONT_CAPTION)
        
        f_box = [right_box[0] + 24, right_box[1] + 115, right_box[2] - 24, right_box[1] + 195]
        draw.rounded_rectangle(f_box, radius=12, fill=(255, 255, 255), outline=(186, 230, 253), width=1)
        draw.text((f_box[0] + 20, f_box[1] + 16), "估值公式: ΔP ≈ - Duration × Δy (中债指数加权)", fill=(2, 132, 199), font=FONT_BODY)
        draw.text((f_box[0] + 20, f_box[1] + 48), "自动追踪十年国债 103.TY00Y 利率波动与组合平均久期", fill=(100, 116, 139), font=FONT_CAPTION)

        b_tags = ["长债/短债智能分类", "人工久期覆盖干预", "基准参考修正算法"]
        for bidx, btag in enumerate(b_tags):
            bx = right_box[0] + 24 + bidx * 230
            by = right_box[1] + 225
            draw.rounded_rectangle([bx, by, bx + 215, by + 50], radius=10, fill=(255, 255, 255), outline=(186, 230, 253), width=1)
            draw.text((bx + 18, by + 14), btag, fill=(3, 105, 161), font=FONT_CAPTION)

    elif sid == 6:
        # 分镜6: 多端响应式 + 企业级控制后台 (免审开关 + 收款码配置)
        draw.text((cx1 + 44, body_y + 10), "手机与电脑双端深度适配，配套全功能企业级可视化管理后台：", fill=(51, 65, 85), font=FONT_BODY)
        w3 = (cx2 - cx1 - 144) // 3

        # 板块1: 多端自适应
        b1 = [cx1 + 44, body_y + 55, cx1 + 44 + w3, body_y + 350]
        draw.rounded_rectangle(b1, radius=16, fill=(239, 246, 255), outline=(191, 219, 254), width=1)
        draw.text((b1[0] + 24, b1[1] + 24), "多端极致适配", fill=(30, 64, 175), font=FONT_CARD_TITLE)
        draw.text((b1[0] + 24, b1[1] + 72), "手机与电脑双向优化", fill=(29, 78, 216), font=FONT_BODY)
        draw.rounded_rectangle([b1[0] + 20, b1[1] + 120, b1[2] - 20, b1[1] + 180], radius=10, fill=(255, 255, 255))
        draw.text((b1[0] + 32, b1[1] + 140), "移动端吸顶 · 绝不折行", fill=(37, 99, 235), font=FONT_BODY)
        draw.text((b1[0] + 24, b1[1] + 205), "• 手机端触摸卡片丝滑流转\n• PC端专业等宽大盘信息密集掌控", fill=(100, 116, 139), font=FONT_CAPTION)

        # 板块2: 免审开关
        b2 = [cx1 + 44 + w3 + 24, body_y + 55, cx1 + 44 + w3 * 2 + 24, body_y + 350]
        draw.rounded_rectangle(b2, radius=16, fill=(240, 253, 244), outline=(187, 247, 208), width=1)
        draw.text((b2[0] + 24, b2[1] + 24), "新用户免审开关", fill=(22, 163, 74), font=FONT_CARD_TITLE)
        draw.text((b2[0] + 24, b2[1] + 72), "一键切换审核/免审模式", fill=(21, 128, 61), font=FONT_BODY)
        draw.rounded_rectangle([b2[0] + 20, b2[1] + 120, b2[2] - 20, b2[1] + 180], radius=10, fill=(255, 255, 255))
        draw.text((b2[0] + 32, b2[1] + 140), "状态: 免审直接登录", fill=(16, 185, 129), font=FONT_BODY)
        draw.text((b2[0] + 24, b2[1] + 205), "• 关闭审核：注册即直接生成账号登录\n• 开启审核：防刷防滥用，手动授权", fill=(100, 116, 139), font=FONT_CAPTION)

        # 板块3: 收款配置
        b3 = [cx1 + 44 + (w3 + 24) * 2, body_y + 55, cx2 - 44, body_y + 350]
        draw.rounded_rectangle(b3, radius=16, fill=(255, 251, 235), outline=(253, 230, 138), width=1)
        draw.text((b3[0] + 24, b3[1] + 24), "VIP价格与收款码", fill=(180, 83, 9), font=FONT_CARD_TITLE)
        draw.text((b3[0] + 24, b3[1] + 72), "微信/支付宝动态配置", fill=(146, 64, 14), font=FONT_BODY)
        draw.rounded_rectangle([b3[0] + 20, b3[1] + 120, b3[2] - 20, b3[1] + 180], radius=10, fill=(255, 255, 255))
        draw.text((b3[0] + 32, b3[1] + 140), "特惠价格仅 ¥2.9 起", fill=(217, 119, 6), font=FONT_BODY)
        draw.text((b3[0] + 24, b3[1] + 205), "• 支持上传收款码或贴图URL实时预览\n• 后台修改，前台秒级动态同步", fill=(100, 116, 139), font=FONT_CAPTION)

    elif sid == 7:
        # 分镜7: 结尾号召与新人福利 (金色皇冠 + 7天VIP体验 + 求三连)
        mid_x = (cx1 + cx2) // 2
        badge_box = [mid_x - 380, body_y + 20, mid_x + 380, body_y + 200]
        draw.rounded_rectangle(badge_box, radius=24, fill=(255, 251, 235), outline=(245, 158, 11), width=2)
        draw.text((mid_x - 290, badge_box[1] + 28), "👑 新用户专属特惠福利", fill=(180, 83, 9), font=FONT_CARD_TITLE)
        draw.text((mid_x - 325, badge_box[1] + 78), "注册即可免费获赠 7 天 VIP 会员体验！", fill=(217, 119, 6), font=FONT_BIG_NUM)
        draw.text((mid_x - 270, badge_box[1] + 144), "盘中持仓收益估算、高频重仓穿透全部免费解锁体验", fill=(146, 64, 14), font=FONT_BODY)

        call_box = [cx1 + 100, body_y + 235, cx2 - 100, body_y + 345]
        draw.rounded_rectangle(call_box, radius=18, fill=(37, 99, 235), outline=(29, 78, 216), width=1)
        draw.text((call_box[0] + 60, call_box[1] + 22), "体验地址与项目源码：请查看【B站置顶评论与视频简介】", fill=(255, 255, 255), font=FONT_CARD_TITLE)
        draw.text((call_box[0] + 60, call_box[1] + 68), "求点赞 · 求投币 · 求收藏  ★  一键三连是对独立开发作者最大的支持！", fill=(219, 234, 254), font=FONT_BODY)

# -------------------------------------------------------------
# 5. 视频构建流水线
# -------------------------------------------------------------
async def build_pipeline():
    print("=====================================================")
    print("🚀 启动 B站 1080P 高清宣传视频全自动渲染流水线 (升级版)")
    print("=====================================================")

    # 1. 逐句生成 TTS 音频并探测时长
    print("\n[1/5] 正在生成微软超拟真活力男声 (zh-CN-YunxiNeural, rate=+22%)...")
    scene_audios = []
    scene_durations = []
    
    for sc in SCENES:
        sid = sc["id"]
        mp3_path = TEMP_DIR / f"voice_{sid}.mp3"
        await generate_speech_file(sc["tts_text"], mp3_path)
        dur = get_audio_duration(mp3_path)
        # 每句结尾保留 0.25 秒微气口
        dur += 0.25
        sc["duration"] = dur
        sc["frames"] = int(dur * FPS)
        scene_audios.append(mp3_path)
        scene_durations.append(dur)
        print(f"  • 分镜 {sid}: 时长 {dur:.2f} 秒 ({sc['frames']} 帧) -> {sc['title']}")

    total_duration = sum(scene_durations)
    total_frames = sum(sc["frames"] for sc in SCENES)
    print(f"\n📊 视频总时长: {total_duration:.2f} 秒 (共 {total_frames} 帧) - 完美控制在 30~45s 黄金完播率内！")

    # 2. 拼接人声并混入 Lo-Fi BGM
    print("\n[2/5] 正在合成全局人声音轨并混音背景配乐...")
    concat_list_file = TEMP_DIR / "voice_concat.txt"
    with open(concat_list_file, "w", encoding="utf-8") as f:
        for p in scene_audios:
            f.write(f"file '{p.resolve().as_posix()}'\n")

    raw_voice_wav = TEMP_DIR / "voice_all.wav"
    subprocess.run([
        "ffmpeg", "-y", "-f", "concat", "-safe", "0",
        "-i", str(concat_list_file),
        "-c:a", "pcm_s16le", str(raw_voice_wav)
    ], stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL, check=True)

    bgm_wav = TEMP_DIR / "bgm.wav"
    generate_lofi_bgm(bgm_wav, total_duration + 1.0)

    mixed_audio_aac = TEMP_DIR / "audio_final.aac"
    subprocess.run([
        "ffmpeg", "-y",
        "-i", str(raw_voice_wav),
        "-i", str(bgm_wav),
        "-filter_complex", "[0:a]volume=1.0[v];[1:a]volume=0.07[m];[v][m]amix=inputs=2:duration=first:dropout_transition=2[out]",
        "-map", "[out]",
        "-c:a", "aac", "-b:a", "192k",
        str(mixed_audio_aac)
    ], stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL, check=True)
    print("✅ 终混音轨已就绪！")

    # 3. 启动 FFmpeg 管道编码器 (1080P 30FPS libx264)
    print("\n[3/5] 正在开启 FFmpeg 1080P libx264 硬件管道...")
    ffmpeg_cmd = [
        "ffmpeg", "-y",
        "-f", "rawvideo",
        "-vcodec", "rawvideo",
        "-s", f"{WIDTH}x{HEIGHT}",
        "-pix_fmt", "rgb24",
        "-r", str(FPS),
        "-i", "-",
        "-i", str(mixed_audio_aac),
        "-c:v", "libx264",
        "-preset", "medium",
        "-crf", "18",
        "-pix_fmt", "yuv420p",
        "-c:a", "copy",
        "-movflags", "+faststart",
        str(FINAL_VIDEO_PATH)
    ]

    pipe = subprocess.Popen(ffmpeg_cmd, stdin=subprocess.PIPE, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)

    # 4. 逐帧渲染并推入管道
    print("\n[4/5] 正在渲染高品质通透卡片帧并压制视频...")
    global_frame = 0
    card_box = [120, 116, WIDTH - 120, 836]

    for sc in SCENES:
        sid = sc["id"]
        frames_count = sc["frames"]
        sub_text = sc["subtitle_text"]
        tag_text = sc["tag"]

        for f_in_scene in range(frames_count):
            img = Image.new("RGB", (WIDTH, HEIGHT), (248, 250, 252))
            draw = ImageDraw.Draw(img, "RGBA")

            # 背景渐变与网格
            draw_gradient_background(draw, global_frame)
            # macOS 风格主窗口卡片 (红黄绿控件)
            draw_main_window(draw, card_box, tag_text)
            # 场景内容
            render_scene_content(draw, sc, card_box, f_in_scene)
            # 动态科技激光扫描线
            draw_laser_scan_line(draw, card_box, global_frame)
            # 悬浮高对比度字幕条
            draw_floating_subtitle_bar(draw, sub_text)

            pipe.stdin.write(img.tobytes())
            global_frame += 1

            if global_frame % 90 == 0:
                percent = (global_frame / total_frames) * 100
                print(f"  Rendering frame {global_frame}/{total_frames} ({percent:.1f}%)...")

    pipe.stdin.close()
    pipe.wait()
    print("✅ 1080P 视频压制输出完毕！")

    # 5. 更新 B站 专属爆款宣发物料
    print("\n[5/5] 正在生成 B站 宣发全套物料...")
    bilibili_kit = """# 基金实时估值系统 · B站专属爆款宣发物料

## 一、 推荐爆款标题（三选一）
1. **【硬核开源】还在盲猜今天亏了几百？自研基金实时估值系统，自选大盘+支付宝4指标联动，盘中收益秒级跳动！** （🔥 强烈推荐 · 痛点直击型）
2. **拒绝收盘盲盒！我给基金写了个日内收益估算器：对标支付宝4大指标，每一分钱都在实时跳动！** （⚡ 科技数码/独立开发型）
3. **基金盘中到底能赚多少钱？手把手带你搭建基金实时估值系统，股票穿透+债券模型+新用户赠7天VIP！** （📈 投资理财/干货教程型）

---

## 二、 视频简介模板
```text
每天盯着基金估算涨跌幅，却算不清今天账户到底到手赚了多少钱？
受够了传统理财 App 纯债基金没有真实估值、盘中盲猜收盘净值的痛苦，我自研打造了这套【基金实时估值系统】！

🔥 核心功能亮点：
1. 【自选基金大盘追踪】：支持代码批量极速添加与自由拖拽排序，盘中秒级追踪全市场标的估值脉搏；
2. 【对标支付宝4大核心指标】：持有金额、昨日收益、持有收益、收益率双向智能联动反算，零门槛同步持仓；
3. 【今日收益秒级测算看板】：大盘涨跌多少，你的钱包盈亏实时跳动，动态市值与累计总盈亏一览无余；
4. 【双核硬核穿透引擎】：
   - 股票型/混合型：实时穿透前十大重仓股秒级行情加权测算；
   - 纯债基金：追踪 10 年期国债收益率 (103.TY00Y) 叠加组合平均久期利率驱动模型测算；
5. 【多端极致自适应】：手机端专属吸顶导航与触摸卡片流，PC端专业等宽大盘掌控；
6. 【企业级管理后台】：新用户一键免审注册、休市日历维护与VIP扫码价格全动态配置；
7. 🎁【新用户专享福利】：注册即送 7 天全功能 VIP 体验！

体验地址与开源代码见下方置顶评论！
喜欢本期视频请务必【点赞、投币、收藏】一键三连，感谢各位小伙伴的支持！
```

---

## 三、 推荐热门标签 (Tags)
`基金` `基金估值` `理财` `支付宝基金` `股票` `投资理财` `独立开发` `开源项目` `Spring Boot` `React` `程序员`

---

## 四、 置顶评论模板
```text
各位小伙伴大家好！感谢大家观看本期视频 🚀

📌 系统体验与相关地址：
👉 在线体验地址：http://daydayfund.dpdns.org/ （或您的部署域名）
👉 项目 GitHub 源码：https://github.com/duyuanfu/fund-valuation
🎁 新人专属特权：新用户注册即可直接获赠【7天 VIP 会员体验】，盘中持仓日内收益测算特权已全量开放！

欢迎在评论区留下您常看的基金代码或功能建议，有问必答！觉得项目不错请务必一键三连支持一下，感谢大家！
```
"""
    with open(PROMO_KIT_PATH, "w", encoding="utf-8") as f:
        f.write(bilibili_kit)

    file_size_mb = FINAL_VIDEO_PATH.stat().st_size / (1024 * 1024)
    print("\n=====================================================")
    print("🎉 B站 1080P 宣传视频制作完成！")
    print(f"📹 视频文件: {FINAL_VIDEO_PATH.resolve()} ({file_size_mb:.2f} MB)")
    print(f"📝 运营物料: {PROMO_KIT_PATH.resolve()}")
    print("=====================================================")

if __name__ == "__main__":
    asyncio.run(build_pipeline())
