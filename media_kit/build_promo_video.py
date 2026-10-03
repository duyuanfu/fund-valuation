#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
基金实时估值系统 - B站 1080P 高清宣传视频全自动构建脚本 (精进重构版)
根据最新反馈深度重构:
1. 彻底删除黑线上下扫动 (去除 laser scan line)；
2. 大幅缩减四周空白 (窗口扩展至 1820x840，内容展示面积扩大 35%+)；
3. 彻底删除管理控制台，专注用户核心看盘与持仓收益体验；
4. 第一帧视觉深度优化：文字极简，大量加入动态波浪曲线、K线动效、呼吸光环与脉冲大数字；
5. 全终端适配画面重构：手机模型展示丰富完整的 4 只基金卡片流，PC端展示高密度专业表格；
6. 字幕彻底摒弃生硬黑底，采用温润浅色微磨砂胶囊；
7. 全程零 VIP 营销词汇，纯粹聚焦硬核产品力与开源分享。
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

# 字体配置 (Windows 微软雅黑)
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

FONT_HERO = get_font(52, bold=True)
FONT_TITLE = get_font(42, bold=True)
FONT_SUBTITLE = get_font(22, bold=False)
FONT_CARD_TITLE = get_font(26, bold=True)
FONT_BIG_NUM = get_font(38, bold=True)
FONT_MEDIUM_NUM = get_font(25, bold=True)
FONT_BODY = get_font(19, bold=False)
FONT_CAPTION = get_font(14, bold=False)
FONT_SUBTITLE_BAR = get_font(23, bold=True)

# 6 个全景精炼分镜 (去除管理后台与VIP，第一帧纯图表视觉冲击，全片 28~32s 黄金节奏)
SCENES = [
    {
        "id": 1,
        "tag": "秒级穿透 · 告别盲盒",
        "title": "自研基金实时估值系统",
        "subtitle": "全市场高频估值穿透 · 每一分钱盘中实时跳动",
        "tts_text": "还在盲猜今天基金亏了几百？自研实时估值系统，告别盘中收益盲盒！",
        "subtitle_text": "还在盲猜今天基金亏了几百？自研实时估值系统，告别盘中收益盲盒！",
    },
    {
        "id": 2,
        "tag": "自选大盘 · 秒级高频",
        "title": "自选基金大盘 · 毫秒级估值脉搏",
        "subtitle": "支持代码批量极速添加 · 自由拖拽排序 · 红绿涨跌胶囊一览无余",
        "tts_text": "首页自选大盘，支持代码批量极速添加与拖拽排序，秒级追踪全市场标的！",
        "subtitle_text": "首页自选大盘，支持代码批量极速添加与拖拽排序，秒级追踪全市场标的！",
    },
    {
        "id": 3,
        "tag": "独家时分 · 收益走势",
        "title": "实时估值曲线 · 盘中分时脉搏图",
        "subtitle": "高频打点绘制分时走势图 · 均线基准对比 · 买卖时机清晰可见",
        "tts_text": "进入基金详情，独家绘制日内分时实时估值曲线，秒级捕捉盘中净值脉搏，买卖时机清晰可见！",
        "subtitle_text": "进入基金详情，独家绘制日内分时实时估值走势图，秒级捕捉买卖时机！",
    },
    {
        "id": 4,
        "tag": "双核穿透 · 拒绝失真",
        "title": "前十大重仓秒级穿透 + 纯债久期利率模型",
        "subtitle": "股票重仓逐只加权穿透 · 纯债跟踪十年期国债利率模型",
        "tts_text": "股票基金秒级穿透前十大重仓股行情；纯债基金依托十年国债利率与久期模型精准测算，杜绝估值失真！",
        "subtitle_text": "股票基金穿透前十大重仓行情，纯债基金依托国债利率与久期模型精准测算！",
    },
    {
        "id": 5,
        "tag": "对标支付宝 · 今日收益",
        "title": "对标支付宝4大指标 · 今日收益实时测算",
        "subtitle": "持有金额、昨日收益、持有收益、收益率 · 今日到手收益与市值实时跳动",
        "tts_text": "持仓录入全面对标支付宝4大核心指标，金额与收益率双向联动，盘中实时推算今日到手收益！",
        "subtitle_text": "持仓录入对标支付宝4大指标，金额与收益率双向联动，实时推算今日收益！",
    },
    {
        "id": 6,
        "tag": "多端自适应 · 开源分享",
        "title": "全终端响应式适配 · 源码开箱即用",
        "subtitle": "手机端吸顶卡片流 + PC端专业大盘 · 体验与源码见置顶评论",
        "tts_text": "手机触摸卡片顺畅流转，电脑大盘专业掌控！体验地址与完整源码见置顶评论，喜欢请一键三连支持一下！",
        "subtitle_text": "手机卡片顺畅流转，电脑大盘专业掌控！体验地址见置顶评论，求三连！",
    },
]

# -------------------------------------------------------------
# 1. edge-tts 语音合成 (带指数退避重试)
# -------------------------------------------------------------
async def generate_speech_file(text: str, output_file: Path, max_retries: int = 3):
    import edge_tts
    for attempt in range(max_retries):
        try:
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
# 2. 生成欢快舒适的轻量科技 Lo-Fi / 电子 BGM (多音轨立体声合成)
# -------------------------------------------------------------
def generate_lofi_bgm(output_wav: Path, total_seconds: float):
    print(f"🎵 正在生成欢快舒适的轻量科技 Lo-Fi / 电子 BGM ({total_seconds:.1f} 秒)...")
    total_samples = int(total_seconds * SAMPLE_RATE)
    
    bpm = 112.0
    beat_dur = 60.0 / bpm
    sixteenth = beat_dur / 4.0
    bar_dur = beat_dur * 4.0  # 4拍一个小节和弦

    # 4小节现代极具阳光活力与科技感的和弦进行 (Fmaj7 - G - Em7 - Am7)
    chord_prog = [
        {"root": 174.61, "notes": [174.61, 261.63, 329.63, 440.00]},  # Fmaj7
        {"root": 196.00, "notes": [196.00, 293.66, 392.00, 493.88]},  # G
        {"root": 164.81, "notes": [164.81, 246.94, 293.66, 392.00]},  # Em7
        {"root": 220.00, "notes": [220.00, 261.63, 329.63, 392.00]},  # Am7
    ]

    # 科技感明快16分音符五声琶音音阶
    arp_scale = [
        523.25, 587.33, 659.25, 783.99, 880.00,
        1046.50, 1174.66, 1318.51, 1567.98, 1760.00
    ]
    num_sixteenths = int(total_seconds / sixteenth) + 16
    pattern = [0, 2, 4, 7, 5, 3, 6, 8, 7, 5, 4, 2, 3, 5, 7, 9]
    arp_notes = [arp_scale[pattern[step % len(pattern)] % len(arp_scale)] for step in range(num_sixteenths)]

    noise_seed = 123456789
    def get_noise():
        nonlocal noise_seed
        noise_seed = (noise_seed * 1103515245 + 12345) & 0x7fffffff
        return (noise_seed / 0x3fffffff) - 1.0

    frames = bytearray()
    for i in range(total_samples):
        t = i / SAMPLE_RATE

        # 1. 和弦电钢琴层 (Warm Electric Piano Plucks)
        chord_idx = int((t / bar_dur) % len(chord_prog))
        cur_chord = chord_prog[chord_idx]
        chord_beat_phase = (t / beat_dur) % 1.0
        chord_env = math.exp(-chord_beat_phase * 4.0) * 0.7 + 0.3 * math.exp(-chord_beat_phase * 1.2)
        chord_sig = 0.0
        for freq in cur_chord["notes"]:
            chord_sig += math.sin(2 * math.pi * freq * t) * 0.06
            chord_sig += math.sin(4 * math.pi * freq * t) * 0.015
            chord_sig += math.sin(6 * math.pi * freq * t) * 0.005
        chord_sig *= chord_env

        # 2. 欢快科技感清脆琶音 (16th-note Marimba / Synth Arp)
        sixteenth_step = int(t / sixteenth)
        sixteenth_phase = (t % sixteenth) / sixteenth
        arp_freq = arp_notes[sixteenth_step % len(arp_notes)]
        arp_env = math.exp(-sixteenth_phase * 12.0)
        arp_sig = (math.sin(2 * math.pi * arp_freq * t) * 0.08 + math.sin(6 * math.pi * arp_freq * t) * 0.02) * arp_env

        # 3. 活跃弹跳贝斯 (Bouncy Synth Bass)
        bass_step = int(t / (beat_dur / 2.0))
        bass_phase = (t % (beat_dur / 2.0)) / (beat_dur / 2.0)
        bass_env = math.exp(-bass_phase * 6.0)
        root_f = cur_chord["root"] / 2.0
        if (bass_step % 4) == 2:
            root_f *= 2.0
        bass_sig = (math.sin(2 * math.pi * root_f * t) * 0.16 + math.sin(4 * math.pi * root_f * t) * 0.05) * bass_env

        # 4. 轻快舒适鼓组 (Lo-Fi Drums: Kick, Snare, Hi-hat)
        beat_idx = int((t / beat_dur) % 4)
        beat_time = (t % beat_dur)
        kick_env = 0.0
        if beat_time < 0.25 and (beat_idx == 0 or beat_idx == 2):
            k_t = beat_time
            k_freq = 110.0 * math.exp(-k_t * 30.0) + 42.0
            kick_env = math.sin(2 * math.pi * k_freq * k_t) * math.exp(-k_t * 18.0) * 0.26
        elif beat_idx == 2 and beat_time > beat_dur * 0.5:
            k_t = beat_time - beat_dur * 0.5
            if k_t < 0.2:
                k_freq = 100.0 * math.exp(-k_t * 30.0) + 42.0
                kick_env = math.sin(2 * math.pi * k_freq * k_t) * math.exp(-k_t * 20.0) * 0.18

        snare_env = 0.0
        if beat_time < 0.22 and (beat_idx == 1 or beat_idx == 3):
            s_t = beat_time
            snare_env = (get_noise() * 0.7 + math.sin(2 * math.pi * 185.0 * s_t) * 0.3) * math.exp(-s_t * 22.0) * 0.15

        hh_phase = (t % (beat_dur / 2.0)) / (beat_dur / 2.0)
        hh_env = get_noise() * math.exp(-hh_phase * 40.0) * 0.05
        drums_sig = kick_env + snare_env + hh_env

        # 5. 立体声总输出
        mix_left = chord_sig * 0.85 + arp_sig * 1.1 + bass_sig * 0.95 + drums_sig * 0.9
        mix_right = chord_sig * 0.95 + arp_sig * 0.85 + bass_sig * 0.95 + drums_sig * 0.9

        val_l = max(-0.95, min(0.95, mix_left))
        val_r = max(-0.95, min(0.95, mix_right))
        frames.extend(struct.pack("<hh", int(val_l * 32767.0), int(val_r * 32767.0)))

    with wave.open(str(output_wav), "w") as wav:
        wav.setnchannels(2)
        wav.setsampwidth(2)
        wav.setframerate(SAMPLE_RATE)
        wav.writeframes(frames)
    print("✅ 欢快轻科技 Lo-Fi BGM 生成完毕！")

# -------------------------------------------------------------
# 3. 现代化视觉组件与动效渲染
# -------------------------------------------------------------
def draw_gradient_background(draw: ImageDraw.ImageDraw, frame_idx: int):
    # 极简柔和渐变底色
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
            draw.point((x, y), fill=(203, 213, 225, 75))

    # 动态彩色柔和环境光
    wave_offset = math.sin(frame_idx * 0.08) * 16
    draw.ellipse([(-80, -80 + wave_offset), (520, 520 + wave_offset)], fill=(224, 242, 254, 40))
    draw.ellipse([(WIDTH - 520, -80 - wave_offset), (WIDTH + 80, 520 - wave_offset)], fill=(245, 243, 255, 35))
    draw.ellipse([(WIDTH - 550, HEIGHT - 550 + wave_offset), (WIDTH + 100, HEIGHT + 100 + wave_offset)], fill=(254, 243, 199, 40))

def draw_main_window(draw: ImageDraw.ImageDraw, card_box, tag_text: str):
    # 显著缩减四周空白！卡片向外扩展至 1820x840
    # 外层柔和阴影模拟
    draw.rounded_rectangle([card_box[0]-4, card_box[1]-4, card_box[2]+4, card_box[3]+4], radius=22, fill=(226, 232, 240, 90))
    draw.rounded_rectangle([card_box[0]-2, card_box[1]-2, card_box[2]+2, card_box[3]+2], radius=20, fill=(241, 245, 249, 140))
    # 主卡片白底
    draw.rounded_rectangle(card_box, radius=18, fill=(255, 255, 255), outline=(226, 232, 240), width=2)

    # macOS 经典红黄绿交通灯微控件 (左上角)
    tx, ty = card_box[0] + 24, card_box[1] + 20
    draw.ellipse([tx, ty, tx + 14, ty + 14], fill=(239, 68, 68))      # 红
    draw.ellipse([tx + 22, ty, tx + 36, ty + 14], fill=(245, 158, 11)) # 黄
    draw.ellipse([tx + 44, ty, tx + 58, ty + 14], fill=(34, 197, 94))  # 绿

    # 顶栏居中 Logo 与系统名
    logo_cx = card_box[0] + 110
    draw.text((logo_cx, ty - 2), "基金实时估值系统 · 盘中毫秒级行情穿透", fill=(15, 23, 42), font=FONT_CAPTION)

    # 右侧特色标签 Pill
    tag_w = 320
    tag_box = [card_box[2] - 24 - tag_w, card_box[1] + 12, card_box[2] - 24, card_box[1] + 44]
    draw.rounded_rectangle(tag_box, radius=16, fill=(239, 246, 255), outline=(191, 219, 254), width=1)
    draw.ellipse([tag_box[0] + 14, tag_box[1] + 12, tag_box[0] + 22, tag_box[1] + 20], fill=(22, 163, 74))
    draw.text((tag_box[0] + 30, tag_box[1] + 7), tag_text, fill=(30, 64, 175), font=FONT_CAPTION)

    # 分割线
    draw.line([(card_box[0], card_box[1] + 54), (card_box[2], card_box[1] + 54)], fill=(241, 245, 249), width=1)

def draw_floating_subtitle_bar(draw: ImageDraw.ImageDraw, text: str):
    # 彻底去除生硬黑色底！改用纯净磨砂白胶囊 + 沉稳深色文字
    sub_w = 1520
    sub_h = 56
    sub_x = (WIDTH - sub_w) // 2
    sub_y = 905
    draw.rounded_rectangle([sub_x, sub_y, sub_x + sub_w, sub_y + sub_h], radius=28, fill=(255, 255, 255, 240), outline=(203, 213, 225), width=1)
    bbox = draw.textbbox((0, 0), text, font=FONT_SUBTITLE_BAR)
    tw = bbox[2] - bbox[0]
    draw.text((sub_x + (sub_w - tw) // 2, sub_y + 13), text, fill=(15, 23, 42), font=FONT_SUBTITLE_BAR)

# -------------------------------------------------------------
# 4. 各场景专属内容高精度排版 (内容区域充分撑满)
# -------------------------------------------------------------
def render_scene_content(draw: ImageDraw.ImageDraw, scene: dict, card_box: list, frame_idx: int):
    sid = scene["id"]
    cx1, cy1, cx2, cy2 = card_box
    content_y = cy1 + 68

    # 顶栏标题区 (统一规范: 42px 粗黑标题 + 22px 浅蓝副标)
    draw.text((cx1 + 40, content_y), scene["title"], fill=(15, 23, 42), font=FONT_TITLE)
    draw.text((cx1 + 42, content_y + 54), scene["subtitle"], fill=(100, 116, 139), font=FONT_SUBTITLE)
    draw.line([(cx1 + 40, content_y + 88), (cx2 - 40, content_y + 88)], fill=(241, 245, 249), width=2)
    
    body_y = content_y + 104

    if sid == 1:
        # 分镜1: 第一帧视觉深度优化 (文字少、大量图表与动态波形、科技光环脉冲)
        # 左侧：大号拟物多层动态光环与科技核心
        icon_cx, icon_cy = cx1 + 260, body_y + 260
        pulse = math.sin(frame_idx * 0.18) * 12
        draw.ellipse([icon_cx - 160 - pulse, icon_cy - 160 - pulse, icon_cx + 160 + pulse, icon_cy + 160 + pulse], fill=(239, 246, 255))
        draw.ellipse([icon_cx - 120, icon_cy - 120, icon_cx + 120, icon_cy + 120], fill=(219, 234, 254), outline=(147, 197, 253), width=2)
        
        # 内部动态 K线与柱状图矩阵 (多根高低起伏彩色柱体)
        bar_colors = [(37, 99, 235), (16, 185, 129), (239, 68, 68), (147, 51, 234), (245, 158, 11)]
        bar_heights = [60, 110, 150, 95, 130]
        for bidx in range(5):
            bx = icon_cx - 80 + bidx * 34
            bh = bar_heights[bidx] + int(math.sin(frame_idx * 0.15 + bidx) * 10)
            draw.rounded_rectangle([bx, icon_cy + 60 - bh, bx + 24, icon_cy + 60], radius=5, fill=bar_colors[bidx])

        # 右侧：华丽大盘收益实时脉搏仪表图
        px = cx1 + 520
        # 顶部大数字突出卡片 (今日收益 +¥386.50 呼吸高光)
        kpi_box = [px, body_y + 10, cx2 - 40, body_y + 160]
        draw.rounded_rectangle(kpi_box, radius=18, fill=(248, 250, 252), outline=(226, 232, 240), width=1)
        draw.text((px + 28, kpi_box[1] + 16), "今日预估到手收益 (实时测算中...)", fill=(100, 116, 139), font=FONT_CAPTION)
        draw.text((px + 28, kpi_box[1] + 46), "+¥ 386.50", fill=(220, 38, 38), font=FONT_HERO)
        draw.text((px + 28, kpi_box[1] + 116), "⚡ 盘中秒级行情加权穿透 · 拒绝晚上开盲盒", fill=(37, 99, 235), font=FONT_BODY)
        
        # 实时动态波动率与总市值标签
        draw.rounded_rectangle([kpi_box[2] - 320, kpi_box[1] + 48, kpi_box[2] - 28, kpi_box[1] + 110], radius=12, fill=(254, 242, 242), outline=(254, 202, 202), width=1)
        draw.text((kpi_box[2] - 300, kpi_box[1] + 62), "日内涨跌: +1.32%", fill=(220, 38, 38), font=FONT_CARD_TITLE)

        # 底部动态时分波浪曲线 (真实的脉搏走势图动效)
        wave_box = [px, body_y + 180, cx2 - 40, body_y + 490]
        draw.rounded_rectangle(wave_box, radius=18, fill=(255, 255, 255), outline=(226, 232, 240), width=1)
        draw.text((wave_box[0] + 24, wave_box[1] + 16), "实时估值动态波浪走势 (高频模拟曲线)", fill=(15, 23, 42), font=FONT_CARD_TITLE)
        
        # 绘制动态正弦脉冲波浪线
        wx1, wy1, wx2, wy2 = wave_box[0] + 30, wave_box[1] + 70, wave_box[2] - 30, wave_box[3] - 40
        wmid_y = wy1 + (wy2 - wy1) // 2
        
        curve_pts = []
        n_pts = 80
        for pidx in range(n_pts):
            c_x = wx1 + (wx2 - wx1) * pidx / (n_pts - 1)
            # 叠加多重正弦波产生极具美感的动态金融曲线
            phase = frame_idx * 0.12
            c_y = wmid_y - math.sin(pidx * 0.15 + phase) * 35 - math.cos(pidx * 0.08) * 20 - (pidx / n_pts) * 40
            curve_pts.append((c_x, c_y))

        # 曲线下方渐变区域
        poly_pts = [(wx1, wy2)] + curve_pts + [(wx2, wy2)]
        draw.polygon(poly_pts, fill=(254, 242, 242, 110))
        # 走势曲线
        for lidx in range(len(curve_pts) - 1):
            draw.line([curve_pts[lidx], curve_pts[lidx + 1]], fill=(220, 38, 38), width=3)
        # 末端脉冲球
        last_pt = curve_pts[-1]
        draw.ellipse([last_pt[0] - 8, last_pt[1] - 8, last_pt[0] + 8, last_pt[1] + 8], fill=(220, 38, 38))
        draw.ellipse([last_pt[0] - 14, last_pt[1] - 14, last_pt[0] + 14, last_pt[1] + 14], outline=(220, 38, 38, 120), width=2)

    elif sid == 2:
        # 分镜2: 自选基金大盘追踪 (批量代码导入 + 拖拽排序 + 实时估算与胶囊徽章)
        tb = [cx1 + 40, body_y + 6, cx2 - 40, body_y + 60]
        draw.rounded_rectangle(tb, radius=12, fill=(248, 250, 252), outline=(226, 232, 240), width=1)
        draw.text((tb[0] + 24, tb[1] + 16), "自选基金大盘库 (共 32 只追踪标的)", fill=(15, 23, 42), font=FONT_BODY)
        
        draw.rounded_rectangle([tb[2] - 340, tb[1] + 8, tb[2] - 210, tb[3] - 8], radius=8, fill=(37, 99, 235))
        draw.text((tb[2] - 324, tb[1] + 13), "+ 批量添加", fill=(255, 255, 255), font=FONT_CAPTION)
        draw.rounded_rectangle([tb[2] - 190, tb[1] + 8, tb[2] - 90, tb[3] - 8], radius=8, fill=(255, 255, 255), outline=(203, 213, 225))
        draw.text((tb[2] - 175, tb[1] + 13), "拖拽排序", fill=(71, 85, 105), font=FONT_CAPTION)
        draw.rounded_rectangle([tb[2] - 70, tb[1] + 8, tb[2] - 15, tb[3] - 8], radius=8, fill=(255, 255, 255), outline=(203, 213, 225))
        draw.text((tb[2] - 56, tb[1] + 13), "刷新", fill=(71, 85, 105), font=FONT_CAPTION)

        # 4行饱满列表
        items = [
            ("易方达消费行业股票", "110022", "主动股票", "2.5500", "+2.00%", "2.5000", "09-29", True),
            ("华夏国证半导体芯片ETF联接", "008888", "指数增强", "1.1280", "+1.15%", "1.1152", "09-29", True),
            ("招商中证白酒指数分级", "161725", "指数型", "0.8920", "+1.48%", "0.8790", "09-29", True),
            ("富国产业债债券A", "100058", "债券型-长债", "1.2385", "-0.12%", "1.2400", "09-29", False),
            ("易方达蓝筹精选混合", "005827", "混合型", "1.8920", "+0.85%", "1.8760", "09-29", True),
        ]
        for idx, (fname, fcode, ftype, estnav, estpct, prevnav, navdate, isup) in enumerate(items):
            iy = body_y + 75 + idx * 72
            ibox = [cx1 + 40, iy, cx2 - 40, iy + 62]
            draw.rounded_rectangle(ibox, radius=10, fill=(255, 255, 255), outline=(226, 232, 240), width=1)
            hx, hy = ibox[0] + 16, iy + 22
            for dx in (0, 6):
                for dy in (0, 7, 14):
                    draw.ellipse([hx + dx, hy + dy, hx + dx + 3, hy + dy + 3], fill=(148, 163, 184))
            
            draw.text((ibox[0] + 45, iy + 12), fname, fill=(15, 23, 42), font=FONT_BODY)
            draw.text((ibox[0] + 45, iy + 36), f"{fcode} · 昨净 {prevnav} ({navdate})", fill=(100, 116, 139), font=FONT_CAPTION)
            
            tag_color = (224, 231, 255) if "指数" in ftype else (254, 226, 226) if "主动" in ftype else (220, 252, 231)
            text_color = (67, 56, 202) if "指数" in ftype else (185, 28, 28) if "主动" in ftype else (21, 128, 61)
            tbox = [ibox[0] + 400, iy + 14, ibox[0] + 500, iy + 42]
            draw.rounded_rectangle(tbox, radius=6, fill=tag_color)
            draw.text((tbox[0] + 12, tbox[1] + 5), ftype[:4], fill=text_color, font=FONT_CAPTION)

            draw.text((ibox[2] - 280, iy + 14), estnav, fill=(220, 38, 38) if isup else (22, 163, 74), font=FONT_MEDIUM_NUM)
            
            bbox = [ibox[2] - 130, iy + 13, ibox[2] - 20, iy + 49]
            draw.rounded_rectangle(bbox, radius=6, fill=(254, 242, 242) if isup else (240, 253, 244), outline=(254, 202, 202) if isup else (187, 247, 208), width=1)
            draw.text((bbox[0] + 18, bbox[1] + 8), estpct, fill=(220, 38, 38) if isup else (22, 163, 74), font=FONT_BODY)

        tip_box = [cx1 + 40, body_y + 445, cx2 - 40, body_y + 505]
        draw.rounded_rectangle(tip_box, radius=12, fill=(240, 249, 255), outline=(186, 230, 253), width=1)
        draw.text((tip_box[0] + 28, tip_box[1] + 18), "⚡ 支持逗号/换行一键批量导入基金代码；拖拽手柄自由排序，全市场基金日内涨跌秒级打点更新！", fill=(3, 105, 161), font=FONT_BODY)

    elif sid == 3:
        # 分镜3: 基金详情页 · 独家实时收益曲线脉搏图 (真实 ECharts 分时折线图模型)
        info_w = 460
        ibox = [cx1 + 40, body_y + 6, cx1 + 40 + info_w, body_y + 490]
        draw.rounded_rectangle(ibox, radius=16, fill=(248, 250, 252), outline=(226, 232, 240), width=1)
        draw.text((ibox[0] + 24, ibox[1] + 22), "易方达消费行业股票 (110022)", fill=(15, 23, 42), font=FONT_CARD_TITLE)
        draw.text((ibox[0] + 24, ibox[1] + 68), "实时估算净值 (14:30 盘中)", fill=(100, 116, 139), font=FONT_CAPTION)
        draw.text((ibox[0] + 24, ibox[1] + 96), "2.5500", fill=(220, 38, 38), font=FONT_HERO)
        
        # 涨跌胶囊
        draw.rounded_rectangle([ibox[0] + 24, ibox[1] + 170, ibox[0] + 160, ibox[1] + 215], radius=8, fill=(254, 242, 242), outline=(254, 202, 202), width=1)
        draw.text((ibox[0] + 42, ibox[1] + 178), "+2.00%", fill=(220, 38, 38), font=FONT_CARD_TITLE)

        draw.text((ibox[0] + 24, ibox[1] + 245), "昨日净值: 2.5000 (09-29)", fill=(71, 85, 105), font=FONT_BODY)
        draw.text((ibox[0] + 24, ibox[1] + 285), "跟踪标的: 消费龙头加权指数", fill=(71, 85, 105), font=FONT_BODY)
        draw.text((ibox[0] + 24, ibox[1] + 325), "持仓报告: 2026年第二季度", fill=(71, 85, 105), font=FONT_BODY)
        draw.text((ibox[0] + 24, ibox[1] + 365), "行情时间: 盘中实时每分钟动态刷新", fill=(71, 85, 105), font=FONT_BODY)
        draw.text((ibox[0] + 24, ibox[1] + 430), "⚡ 自动记录日内估值脉搏波形走势", fill=(37, 99, 235), font=FONT_BODY)

        # 右侧图表区域 (高保真 ECharts 时分折线走势图)
        chart_box = [cx1 + 40 + info_w + 24, body_y + 6, cx2 - 40, body_y + 490]
        draw.rounded_rectangle(chart_box, radius=16, fill=(255, 255, 255), outline=(226, 232, 240), width=1)
        draw.text((chart_box[0] + 24, chart_box[1] + 20), "日内实时估值走势图 (分时脉搏折线)", fill=(15, 23, 42), font=FONT_CARD_TITLE)

        # 坐标网格虚线
        gx1, gy1, gx2, gy2 = chart_box[0] + 60, chart_box[1] + 85, chart_box[2] - 40, chart_box[3] - 70
        draw.rectangle([gx1, gy1, gx2, gy2], outline=(241, 245, 249), width=1)
        for y_line in range(gy1, gy2, 60):
            draw.line([(gx1, y_line), (gx2, y_line)], fill=(241, 245, 249), width=1)
        mid_y = gy1 + (gy2 - gy1) // 2
        draw.line([(gx1, mid_y), (gx2, mid_y)], fill=(203, 213, 225), width=1)
        draw.text((gx1 + 10, mid_y - 20), "昨日收盘基准 (2.5000)", fill=(148, 163, 184), font=FONT_CAPTION)

        # 平滑红线上扬走势曲线
        curve_pts = []
        n_pts = 70
        for pidx in range(n_pts):
            px = gx1 + (gx2 - gx1) * pidx / (n_pts - 1)
            trend = math.sin(pidx * 0.15) * 25 + (pidx / n_pts) * 90
            py = mid_y - trend + 20
            curve_pts.append((px, py))

        poly_pts = [(gx1, gy2)] + curve_pts + [(gx2, gy2)]
        draw.polygon(poly_pts, fill=(254, 242, 242, 120))
        for lidx in range(len(curve_pts) - 1):
            draw.line([curve_pts[lidx], curve_pts[lidx + 1]], fill=(220, 38, 38), width=3)

        last_pt = curve_pts[-1]
        draw.ellipse([last_pt[0] - 8, last_pt[1] - 8, last_pt[0] + 8, last_pt[1] + 8], fill=(220, 38, 38))
        draw.ellipse([last_pt[0] - 14, last_pt[1] - 14, last_pt[0] + 14, last_pt[1] + 14], outline=(220, 38, 38, 120), width=2)
        
        tbox = [last_pt[0] - 180, last_pt[1] - 50, last_pt[0] - 10, last_pt[1] - 8]
        draw.rounded_rectangle(tbox, radius=6, fill=(15, 23, 42))
        draw.text((tbox[0] + 14, tbox[1] + 8), "实时估值: 2.5500", fill=(255, 255, 255), font=FONT_CAPTION)

        times = ["09:30", "10:30", "11:30/13:00", "14:00", "15:00"]
        for tidx, tm in enumerate(times):
            tx_label = gx1 + (gx2 - gx1) * tidx / (len(times) - 1) - 20
            draw.text((tx_label, gy2 + 12), tm, fill=(100, 116, 139), font=FONT_CAPTION)

    elif sid == 4:
        # 分镜4: 前十大重仓股秒级穿透 + 纯债久期利率驱动模型
        half_w = (cx2 - cx1 - 104) // 2
        # 左栏: 股票基金前十大重仓穿透
        left_box = [cx1 + 40, body_y + 6, cx1 + 40 + half_w, body_y + 490]
        draw.rounded_rectangle(left_box, radius=16, fill=(248, 250, 252), outline=(203, 213, 225), width=1)
        draw.text((left_box[0] + 24, left_box[1] + 20), "股票型/混合型 · 穿透前十大重仓", fill=(30, 41, 59), font=FONT_CARD_TITLE)
        draw.text((left_box[0] + 24, left_box[1] + 62), "直连主流交易所极速行情，按季度报告持仓权重逐只穿透加权：", fill=(100, 116, 139), font=FONT_CAPTION)
        
        stocks = [
            ("贵州茅台 (600519)", "9.85%", "+1.65%", 110),
            ("宁德时代 (300750)", "8.42%", "+2.30%", 94),
            ("腾讯控股 (00700)", "7.10%", "-0.45%", 78),
            ("美的集团 (000333)", "5.25%", "+0.92%", 58),
            ("五粮液 (000858)", "4.80%", "+1.80%", 52),
        ]
        for sidx, (sname, sw, spct, sbar_w) in enumerate(stocks):
            sy = left_box[1] + 100 + sidx * 72
            srow = [left_box[0] + 20, sy, left_box[2] - 20, sy + 62]
            draw.rounded_rectangle(srow, radius=8, fill=(255, 255, 255), outline=(226, 232, 240), width=1)
            draw.text((srow[0] + 16, sy + 11), sname, fill=(15, 23, 42), font=FONT_BODY)
            draw.text((srow[0] + 16, sy + 35), f"持仓占比 {sw}", fill=(100, 116, 139), font=FONT_CAPTION)
            draw.rounded_rectangle([srow[0] + 150, sy + 38, srow[0] + 150 + sbar_w, sy + 46], radius=4, fill=(37, 99, 235))
            is_up = "+" in spct
            draw.text((srow[2] - 120, sy + 18), spct, fill=(220, 38, 38) if is_up else (22, 163, 74), font=FONT_MEDIUM_NUM)

        # 右栏: 纯债基金利率驱动模型
        right_box = [cx1 + 40 + half_w + 24, body_y + 6, cx2 - 40, body_y + 490]
        draw.rounded_rectangle(right_box, radius=16, fill=(240, 249, 255), outline=(186, 230, 253), width=1)
        draw.text((right_box[0] + 24, right_box[1] + 20), "纯债基金 · 10年期国债利率驱动模型", fill=(3, 105, 161), font=FONT_CARD_TITLE)
        draw.text((right_box[0] + 24, right_box[1] + 62), "纯债不公开持仓秒级行情，系统采用久期与国债利率实时测算：", fill=(100, 116, 139), font=FONT_CAPTION)
        
        f_box = [right_box[0] + 20, right_box[1] + 100, right_box[2] - 20, right_box[1] + 230]
        draw.rounded_rectangle(f_box, radius=12, fill=(255, 255, 255), outline=(186, 230, 253), width=1)
        draw.text((f_box[0] + 20, f_box[1] + 18), "估值公式: ΔP ≈ - Duration × Δy (中债指数加权算法)", fill=(2, 132, 199), font=FONT_BODY)
        draw.text((f_box[0] + 20, f_box[1] + 54), "• 跟踪基准: 10 年期国债活跃收益率 (103.TY00Y)\n• 组合平均久期: 3.8 年  |  基准参考久期: 10.0 年\n• 自动匹配定期报告平均久期，日内利率波动高频推导", fill=(71, 85, 105), font=FONT_BODY)

        b_tags = ["长债/短债智能模型分类", "人工久期覆盖干预校准", "杜绝传统纯债估值严重失真"]
        for bidx, btag in enumerate(b_tags):
            by = right_box[1] + 255 + bidx * 60
            draw.rounded_rectangle([right_box[0] + 20, by, right_box[2] - 20, by + 50], radius=8, fill=(255, 255, 255), outline=(186, 230, 253), width=1)
            draw.text((right_box[0] + 36, by + 14), f"✓  {btag}", fill=(3, 105, 161), font=FONT_BODY)

    elif sid == 5:
        # 分镜5: 对标支付宝4大指标 + 今日到手收益实时测算看板
        col_w = 408
        cards = [
            ("今日收益 (盘中实时)", "+¥ 386.50", "日内涨跌: +1.32%", (220, 38, 38), (254, 242, 242)),
            ("总市值 (动态现值)", "¥ 68,520.00", "基准金额: ¥68,133.50", (15, 23, 42), (255, 255, 255)),
            ("累计盈亏", "+¥ 4,280.00", "总回报率: +6.67%", (220, 38, 38), (255, 255, 255)),
            ("昨日收益 (官方)", "+¥ 120.00", "支付宝账面确认到账", (15, 23, 42), (255, 255, 255)),
        ]
        for idx, (cname, cval, csub, ccol, cbg) in enumerate(cards):
            bx = cx1 + 40 + idx * (col_w + 34)
            bbox = [bx, body_y + 6, bx + col_w, body_y + 175]
            draw.rounded_rectangle(bbox, radius=14, fill=cbg, outline=(226, 232, 240), width=1)
            draw.text((bx + 24, bbox[1] + 18), cname, fill=(100, 116, 139), font=FONT_CAPTION)
            draw.text((bx + 24, bbox[1] + 52), cval, fill=ccol, font=FONT_BIG_NUM)
            draw.text((bx + 24, bbox[1] + 124), csub, fill=(100, 116, 139), font=FONT_CAPTION)

        # 对标支付宝4指标输入联动横幅
        abox = [cx1 + 40, body_y + 195, cx2 - 40, body_y + 490]
        draw.rounded_rectangle(abox, radius=16, fill=(248, 250, 252), outline=(226, 232, 240), width=1)
        draw.text((abox[0] + 28, abox[1] + 22), "录入对标支付宝 4 大核心指标 (智能双向推算):", fill=(30, 41, 59), font=FONT_CARD_TITLE)
        
        f_cols = [("持有金额", "¥ 20,000.00"), ("昨日收益", "+¥ 50.00"), ("持有收益", "+¥ 1,500.00"), ("持有收益率", "+8.11%")]
        for fidx, (fk, fv) in enumerate(f_cols):
            fx = abox[0] + 28 + fidx * 410
            fbox = [fx, abox[1] + 70, fx + 380, abox[1] + 165]
            draw.rounded_rectangle(fbox, radius=12, fill=(255, 255, 255), outline=(226, 232, 240), width=1)
            draw.text((fx + 20, fbox[1] + 16), fk, fill=(100, 116, 139), font=FONT_CAPTION)
            draw.text((fx + 20, fbox[1] + 46), fv, fill=(15, 23, 42), font=FONT_BIG_NUM)

        draw.text((abox[0] + 28, abox[1] + 200), "⚡ 输入金额与收益，自动推算收益率与持仓份额；盘中每分钟结合估值引擎动态推算到手收益！", fill=(37, 99, 235), font=FONT_BODY)
        draw.text((abox[0] + 28, abox[1] + 236), "💡 零门槛记账，数据模型全面预留未来支付宝截图 OCR 自动扫描识别一键导入能力！", fill=(71, 85, 105), font=FONT_BODY)

    elif sid == 6:
        # 分镜6: 多端响应式深度适配 (手机卡片流 4 只基金 + 电脑全量专业大盘)
        draw.text((cx1 + 40, body_y + 6), "全终端深度自适应，手机与电脑始终呈现最高清晰度与触感：", fill=(51, 65, 85), font=FONT_BODY)
        
        # 手机端模型: 4只基金丰富卡片流
        phone_box = [cx1 + 60, body_y + 45, cx1 + 540, body_y + 490]
        draw.rounded_rectangle(phone_box, radius=24, fill=(15, 23, 42), outline=(51, 65, 85), width=2)
        draw.rounded_rectangle([phone_box[0] + 10, phone_box[1] + 14, phone_box[2] - 10, phone_box[3] - 14], radius=16, fill=(248, 250, 252))
        draw.text((phone_box[0] + 24, phone_box[1] + 22), "自选基金  |  持仓估值", fill=(15, 23, 42), font=FONT_CAPTION)
        
        # 手机内 4 只真实基金卡片
        m_funds = [
            ("易方达消费行业", "+¥260.00", "+1.30%", "¥20,000", True),
            ("华夏半导体芯片", "+¥126.50", "+0.84%", "¥15,000", True),
            ("招商中证白酒", "+¥180.00", "+1.20%", "¥15,000", True),
            ("富国产业债A", "-¥15.00", "-0.10%", "¥15,000", False),
        ]
        for midx, (mname, mincome, mpct, mamt, misup) in enumerate(m_funds):
            my = phone_box[1] + 50 + midx * 82
            mbox = [phone_box[0] + 16, my, phone_box[2] - 16, my + 72]
            draw.rounded_rectangle(mbox, radius=10, fill=(255, 255, 255), outline=(226, 232, 240), width=1)
            draw.text((mbox[0] + 14, my + 10), mname, fill=(15, 23, 42), font=FONT_CAPTION)
            draw.text((mbox[0] + 14, my + 38), f"持有 {mamt}", fill=(100, 116, 139), font=FONT_CAPTION)
            draw.text((mbox[2] - 140, my + 10), mincome, fill=(220, 38, 38) if misup else (22, 163, 74), font=FONT_MEDIUM_NUM)
            draw.text((mbox[2] - 110, my + 40), mpct, fill=(220, 38, 38) if misup else (22, 163, 74), font=FONT_CAPTION)

        draw.text((phone_box[0] + 20, phone_box[3] - 34), "⚡ 手机端独立吸顶导航 · 绝不挤压折行", fill=(37, 99, 235), font=FONT_CAPTION)

        # PC 端宽屏专业表格 (多字段真实高密度展示)
        pc_box = [cx1 + 570, body_y + 45, cx2 - 40, body_y + 360]
        draw.rounded_rectangle(pc_box, radius=16, fill=(255, 255, 255), outline=(203, 213, 225), width=1)
        draw.rounded_rectangle([pc_box[0], pc_box[1], pc_box[2], pc_box[1] + 46], radius=14, fill=(241, 245, 249))
        draw.text((pc_box[0] + 24, pc_box[1] + 13), "基金名称 / 代码            持有金额        昨日收益      今日收益      最新累计盈亏", fill=(71, 85, 105), font=FONT_BODY)
        
        rows = [
            ("易方达消费行业 (110022)", "¥ 20,000.00", "+¥ 50.00", "+¥ 260.00 (+1.30%)", "+¥ 1,760.00"),
            ("华夏半导体芯片 (008888)", "¥ 15,000.00", "-¥ 30.00", "+¥ 126.50 (+0.84%)", "+¥ 980.00"),
            ("招商中证白酒增 (161725)", "¥ 15,000.00", "+¥ 40.00", "+¥ 180.00 (+1.20%)", "+¥ 1,420.00"),
            ("富国产业债债券 (100058)", "¥ 15,000.00", "+¥ 10.00", "-¥ 15.00 (-0.10%)", "+¥ 350.00"),
        ]
        for ridx, (rname, ramt, ryest, rtoday, rtotal) in enumerate(rows):
            ry = pc_box[1] + 54 + ridx * 62
            draw.text((pc_box[0] + 24, ry + 12), f"{rname}    {ramt}    {ryest}    {rtoday}    {rtotal}", fill=(15, 23, 42), font=FONT_BODY)
            draw.line([(pc_box[0] + 14, ry + 52), (pc_box[2] - 14, ry + 52)], fill=(241, 245, 249), width=1)

        # 底部三连号召横幅
        call_box = [cx1 + 570, body_y + 380, cx2 - 40, body_y + 490]
        draw.rounded_rectangle(call_box, radius=16, fill=(37, 99, 235), outline=(29, 78, 216), width=1)
        draw.text((call_box[0] + 36, call_box[1] + 22), "在线体验与完整源码：请查看【B站置顶评论与视频简介】", fill=(255, 255, 255), font=FONT_CARD_TITLE)
        draw.text((call_box[0] + 36, call_box[1] + 64), "求点赞 · 求投币 · 求收藏  ★  一键三连是对独立开发作者最大的支持！", fill=(219, 234, 254), font=FONT_BODY)

# -------------------------------------------------------------
# 5. 视频构建流水线
# -------------------------------------------------------------
async def build_pipeline():
    print("=====================================================")
    print("🚀 启动 B站 1080P 高清宣传视频全自动渲染流水线 (精进重构版)")
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
        dur += 0.15
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
        "-filter_complex", "[0:a]volume=1.0[v];[1:a]volume=0.075[m];[v][m]amix=inputs=2:duration=first:dropout_transition=2[out]",
        "-map", "[out]",
        "-c:a", "aac", "-b:a", "192k",
        str(mixed_audio_aac)
    ], stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL, check=True)
    print("✅ 终混音轨已就绪！")

    # 3. 启动 FFmpeg 管道编码器
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

    # 4. 逐帧渲染并推入管道 (卡片扩容至 1820x840，去除黑线扫描动效)
    print("\n[4/5] 正在渲染高品质通透卡片帧并压制视频...")
    global_frame = 0
    # 四周空白显著缩减: 左右边距仅 50px，顶部边距 40px，窗口满幅呈现
    card_box = [50, 40, WIDTH - 50, 880]

    for sc in SCENES:
        sid = sc["id"]
        frames_count = sc["frames"]
        sub_text = sc["subtitle_text"]
        tag_text = sc["tag"]

        for f_in_scene in range(frames_count):
            img = Image.new("RGB", (WIDTH, HEIGHT), (248, 250, 252))
            draw = ImageDraw.Draw(img, "RGBA")

            # 浅色通透渐变与网格背景
            draw_gradient_background(draw, global_frame)
            # macOS 经典窗口控件 (满幅大卡片)
            draw_main_window(draw, card_box, tag_text)
            # 场景核心排版
            render_scene_content(draw, sc, card_box, f_in_scene)
            # 悬浮高透浅色字幕条 (绝无显眼黑底)
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
1. **【硬核开源】大盘剧烈波动还在盲猜收盘？自研基金实时估值系统，自选大盘+时分走势+重仓穿透，盘中收益实时跳动！** （🔥 强烈推荐 · 痛点直击型）
2. **拒绝收盘开盲盒！我给基金写了个实时估值神器：分时收益曲线+十大重仓穿透+对标支付宝4指标！** （⚡ 科技数码/独立开发型）
3. **基金盘中到底能赚多少钱？手把手带你搭建自研基金实时估值系统，股票穿透+债券久期+多端极速自适应！** （📈 投资理财/干货教程型）

---

## 二、 视频简介模板
```text
白天大盘波动剧烈，却看不了实时精确估值，只能等晚上收盘开盲盒？
受够了传统理财 App 纯债基金没有真实估值、无法量化今日到手具体赚了多少钱的痛苦，我自研打造了这套【基金实时估值系统】！

🔥 核心功能全景介绍：
1. 【自选大盘毫秒级追踪】：支持代码批量极速添加与拖拽排序，全市场标的高频监控；
2. 【独家时分实时估值曲线】：时分折线走势脉搏图，均线对比打点，秒级捕捉买卖时机；
3. 【双核硬核穿透引擎】：
   - 股票型/混合型：实时穿透前十大重仓股秒级行情与占比权重；
   - 纯债基金：追踪 10 年期国债收益率 (103.TY00Y) 叠加组合平均久期利率驱动模型精准测算，杜绝估值失真；
4. 【对标支付宝4大指标】：持有金额、昨日收益、持有收益、收益率双向联动，盘中实时推算今日到手收益；
5. 【全端极致响应式适配】：手机端专属吸顶导航与4只基金卡片流，电脑端专业高密度等宽大盘。

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
👉 在线体验地址：http://trythis.pw
👉 项目 GitHub 源码：https://github.com/duyuanfu/fund-valuation

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
