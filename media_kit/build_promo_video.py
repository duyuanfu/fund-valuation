#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
基金实时估值系统 - B站百万爆款流量级宣传视频构建流水线 (高级舒适浅色主题版)
设计规范:
1. 视觉升级: 顶级 SaaS / Apple 级舒适浅色金融风 (Clean Light Finance)，柔和微渐变底 + 纯白大圆角卡片 + 优雅微投影；
2. 经典舒适红绿: 涨跌采用清爽耐看的胶囊徽章 (浅红底+正红字 #ef4444 / 浅绿底+翠绿字 #16a34a)；
3. 独家时分走势图: 优雅珊瑚红折线 + 柔和温润渐变填充 + 呼吸脉冲球；
4. 叙事节奏: 保持黄金完播率高流量逻辑 (痛点暴击 -> 自研神器 -> 分时走势 -> 重仓利率穿透 -> 秒算今日到手 -> 全网开源)；
5. 字幕美学: 悬浮微磨砂纯白胶囊 + 沉稳深色标题文字，阅读极其温润舒适。
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

FONT_HERO = get_font(50, bold=True)
FONT_TITLE = get_font(40, bold=True)
FONT_SUBTITLE = get_font(21, bold=False)
FONT_CARD_TITLE = get_font(25, bold=True)
FONT_BIG_NUM = get_font(36, bold=True)
FONT_MEDIUM_NUM = get_font(24, bold=True)
FONT_BODY = get_font(18, bold=False)
FONT_CAPTION = get_font(13, bold=False)
FONT_SUBTITLE_BAR = get_font(24, bold=True)

# 6 个爆款节奏分镜 (总时长约 48s)
SCENES = [
    {
        "id": 1,
        "tag": "⚡ 痛点暴击 · 告别盲盒",
        "title": "官方估值全面下架，下午2点半还在盲猜？",
        "subtitle": "大盘剧烈上蹿下跳 · 盘中两眼一抹黑 · 晚上八点被迫开盲盒！",
        "tts_text": "官方估值下线后，你是不是也每天下午两点半看着大盘乱跳，手里基金是赚是亏全靠盲猜？",
        "subtitle_text": "官方估值下线后，你是不是也每天下午两点半看着大盘乱跳，收益全靠盲猜？",
    },
    {
        "id": 2,
        "tag": "🚀 自研神器 · 秒级脉搏",
        "title": "自研基金盘中实时估值系统上线！",
        "subtitle": "直通交易所极速撮合行情 · 批量导入 · 每一分钱盘中实时跳动！",
        "tts_text": "为了告别被动挨打，我花了半个月自研了这套实时估值神器！秒级高频穿透，盘中盈亏全透明！",
        "subtitle_text": "为了告别被动挨打，自研这套实时估值神器！秒级高频穿透，盘中盈亏全透明！",
    },
    {
        "id": 3,
        "tag": "📈 独家神技 · 分时走势",
        "title": "独家绘制日内实时分时脉冲曲线",
        "subtitle": "全天240点动态分时折线 · 均线基准对比 · 最佳买卖点清晰可见！",
        "tts_text": "别人只有收盘数字，我们直接绘制全天实时分时曲线！秒级捕捉涨跌脉冲，买卖时机一清二楚！",
        "subtitle_text": "别人只有收盘数字，我们直接绘制全天实时分时曲线，买卖时机一清二楚！",
    },
    {
        "id": 4,
        "tag": "🔬 硬核双核 · 极速穿透",
        "title": "股票前十大重仓穿透 + 纯债利率久期模型",
        "subtitle": "股票逐只权重加权穿透 · 纯债跟踪十年期国债收益率，杜绝失真！",
        "tts_text": "股票基金逐只穿透前十大重仓股票；纯债基金依托十年国债利率与久期模型动态推算，告别恒定为零！",
        "subtitle_text": "股票穿透前十大重仓，纯债依托国债利率与久期模型动态推算，拒绝失真！",
    },
    {
        "id": 5,
        "tag": "💰 收益立现 · 秒算到手",
        "title": "对标支付宝4大指标 · 实时推算今日到手",
        "subtitle": "持有金额与份额双向联动 · 盘中实时测算今天具体赚了多少钱！",
        "tts_text": "对标支付宝4大核心指标，金额份额自动联动，盘中每分钟直接推算出今天实收赚了多少钱！",
        "subtitle_text": "对标支付宝4大核心指标，盘中每分钟直接推算出今天到手赚了多少钱！",
    },
    {
        "id": 6,
        "tag": "🎁 全网开源 · 诚意分享",
        "title": "手机电脑双端自适应 · 代码全量免费开源",
        "subtitle": "在线秒开开箱即用 · 体验地址与GitHub源码见置顶评论！",
        "tts_text": "手机电脑全适配！在线体验与全部源代码免费开源，地址见置顶评论，觉得好用求一键三连支持！",
        "subtitle_text": "手机电脑全适配！体验地址与完整源码见置顶评论，求一键三连支持！",
    },
]

# -------------------------------------------------------------
# 1. edge-tts 语音合成 (超拟真活力男声)
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
# 2. 动感科技 Lo-Fi / 电子 BGM 生成 (116 BPM 律动和弦)
# -------------------------------------------------------------
def generate_lofi_bgm(output_wav: Path, total_seconds: float):
    print(f"🎵 正在生成轻盈舒适的科技 Lo-Fi / 电子 BGM ({total_seconds:.1f} 秒)...")
    total_samples = int(total_seconds * SAMPLE_RATE)
    
    bpm = 116.0
    beat_dur = 60.0 / bpm
    sixteenth = beat_dur / 4.0
    bar_dur = beat_dur * 4.0

    chord_prog = [
        {"root": 220.00, "notes": [220.00, 261.63, 329.63, 392.00]},  # Am7
        {"root": 174.61, "notes": [174.61, 261.63, 329.63, 440.00]},  # Fmaj7
        {"root": 130.81, "notes": [261.63, 329.63, 392.00, 523.25]},  # C
        {"root": 196.00, "notes": [196.00, 246.94, 293.66, 392.00]},  # G
    ]

    arp_scale = [
        523.25, 587.33, 659.25, 783.99, 880.00,
        1046.50, 1174.66, 1318.51, 1567.98, 1760.00
    ]
    num_sixteenths = int(total_seconds / sixteenth) + 16
    pattern = [0, 3, 5, 7, 4, 2, 6, 8, 7, 5, 3, 2, 4, 6, 7, 9]
    arp_notes = [arp_scale[pattern[step % len(pattern)] % len(arp_scale)] for step in range(num_sixteenths)]

    noise_seed = 987654321
    def get_noise():
        nonlocal noise_seed
        noise_seed = (noise_seed * 1103515245 + 12345) & 0x7fffffff
        return (noise_seed / 0x3fffffff) - 1.0

    frames = bytearray()
    for i in range(total_samples):
        t = i / SAMPLE_RATE

        # 电钢琴和弦拨弦
        chord_idx = int((t / bar_dur) % len(chord_prog))
        cur_chord = chord_prog[chord_idx]
        chord_beat_phase = (t / beat_dur) % 1.0
        chord_env = math.exp(-chord_beat_phase * 4.2) * 0.75 + 0.25 * math.exp(-chord_beat_phase * 1.0)
        chord_sig = 0.0
        for freq in cur_chord["notes"]:
            chord_sig += math.sin(2 * math.pi * freq * t) * 0.065
            chord_sig += math.sin(4 * math.pi * freq * t) * 0.018
        chord_sig *= chord_env

        # 清亮电子琶音
        sixteenth_step = int(t / sixteenth)
        sixteenth_phase = (t % sixteenth) / sixteenth
        arp_freq = arp_notes[sixteenth_step % len(arp_notes)]
        arp_env = math.exp(-sixteenth_phase * 14.0)
        arp_sig = (math.sin(2 * math.pi * arp_freq * t) * 0.085 + math.sin(6 * math.pi * arp_freq * t) * 0.02) * arp_env

        # 弹跳贝斯
        bass_step = int(t / (beat_dur / 2.0))
        bass_phase = (t % (beat_dur / 2.0)) / (beat_dur / 2.0)
        bass_env = math.exp(-bass_phase * 6.5)
        root_f = cur_chord["root"] / 2.0
        if (bass_step % 4) == 2:
            root_f *= 1.5
        bass_sig = (math.sin(2 * math.pi * root_f * t) * 0.18 + math.sin(4 * math.pi * root_f * t) * 0.05) * bass_env

        # 鼓组
        beat_idx = int((t / beat_dur) % 4)
        beat_time = (t % beat_dur)
        kick_env = 0.0
        if beat_time < 0.22 and (beat_idx == 0 or beat_idx == 2):
            k_t = beat_time
            k_freq = 120.0 * math.exp(-k_t * 32.0) + 45.0
            kick_env = math.sin(2 * math.pi * k_freq * k_t) * math.exp(-k_t * 20.0) * 0.28
        elif beat_idx == 2 and beat_time > beat_dur * 0.5:
            k_t = beat_time - beat_dur * 0.5
            if k_t < 0.18:
                k_freq = 110.0 * math.exp(-k_t * 32.0) + 45.0
                kick_env = math.sin(2 * math.pi * k_freq * k_t) * math.exp(-k_t * 22.0) * 0.20

        snare_env = 0.0
        if beat_time < 0.22 and (beat_idx == 1 or beat_idx == 3):
            s_t = beat_time
            snare_env = (get_noise() * 0.75 + math.sin(2 * math.pi * 190.0 * s_t) * 0.25) * math.exp(-s_t * 22.0) * 0.16

        hh_phase = (t % (beat_dur / 2.0)) / (beat_dur / 2.0)
        hh_env = get_noise() * math.exp(-hh_phase * 45.0) * 0.05
        drums_sig = kick_env + snare_env + hh_env

        mix_left = chord_sig * 0.9 + arp_sig * 1.1 + bass_sig * 0.95 + drums_sig * 0.9
        mix_right = chord_sig * 0.95 + arp_sig * 0.9 + bass_sig * 0.95 + drums_sig * 0.9

        val_l = max(-0.95, min(0.95, mix_left))
        val_r = max(-0.95, min(0.95, mix_right))
        frames.extend(struct.pack("<hh", int(val_l * 32767.0), int(val_r * 32767.0)))

    with wave.open(str(output_wav), "w") as wav:
        wav.setnchannels(2)
        wav.setsampwidth(2)
        wav.setframerate(SAMPLE_RATE)
        wav.writeframes(frames)
    print("✅ 动感科技 Lo-Fi BGM 生成完毕！")

# -------------------------------------------------------------
# 3. 高级舒适浅色金融风 (Clean Light Finance) 视觉渲染
# -------------------------------------------------------------
def draw_gradient_background(draw: ImageDraw.ImageDraw, frame_idx: int):
    # 温润柔和的浅色微渐变 (#f8fafc -> #f1f5f9)
    for y in range(0, HEIGHT, 2):
        ratio = y / HEIGHT
        r = int(248 * (1 - ratio) + 241 * ratio)
        g = int(250 * (1 - ratio) + 245 * ratio)
        b = int(252 * (1 - ratio) + 249 * ratio)
        draw.line([(0, y), (WIDTH, y)], fill=(r, g, b), width=2)

    # 极细舒适微点阵
    grid_gap = 38
    for x in range(20, WIDTH, grid_gap):
        for y in range(20, HEIGHT, grid_gap):
            draw.point((x, y), fill=(203, 213, 225, 90))

    # 动态柔和呼吸光晕 (浅天蓝与浅紫霞光)
    wave_offset = math.sin(frame_idx * 0.08) * 16
    draw.ellipse([(-100, -100 + wave_offset), (520, 520 + wave_offset)], fill=(224, 242, 254, 45))
    draw.ellipse([(WIDTH - 550, -100 - wave_offset), (WIDTH + 100, 550 - wave_offset)], fill=(238, 242, 255, 40))
    draw.ellipse([(WIDTH - 580, HEIGHT - 550 + wave_offset), (WIDTH + 120, HEIGHT + 120 + wave_offset)], fill=(254, 243, 199, 45))

def draw_main_window(draw: ImageDraw.ImageDraw, card_box, tag_text: str):
    # 柔和三层微阴影模拟 (精致漂浮感)
    draw.rounded_rectangle([card_box[0]-4, card_box[1]-4, card_box[2]+4, card_box[3]+4], radius=22, fill=(226, 232, 240, 80))
    draw.rounded_rectangle([card_box[0]-2, card_box[1]-2, card_box[2]+2, card_box[3]+2], radius=20, fill=(241, 245, 249, 130))
    # 主卡片白底
    draw.rounded_rectangle(card_box, radius=18, fill=(255, 255, 255), outline=(226, 232, 240), width=2)

    # macOS 经典红黄绿交通灯 (左上角)
    tx, ty = card_box[0] + 28, card_box[1] + 20
    draw.ellipse([tx, ty, tx + 14, ty + 14], fill=(239, 68, 68))
    draw.ellipse([tx + 22, ty, tx + 36, ty + 14], fill=(245, 158, 11))
    draw.ellipse([tx + 44, ty, tx + 58, ty + 14], fill=(34, 197, 94))

    # 顶栏居中 Logo 与系统名 (沉稳深蓝灰)
    logo_cx = card_box[0] + 120
    draw.text((logo_cx, ty - 2), "🔥 基金实时估值系统 · 盘中毫秒级行情穿透神器", fill=(15, 23, 42), font=FONT_CAPTION)

    # 右侧特色标签 Pill (舒适浅蓝微底)
    tag_w = 340
    tag_box = [card_box[2] - 28 - tag_w, card_box[1] + 12, card_box[2] - 28, card_box[1] + 44]
    draw.rounded_rectangle(tag_box, radius=16, fill=(239, 246, 255), outline=(191, 219, 254), width=1)
    draw.ellipse([tag_box[0] + 16, tag_box[1] + 12, tag_box[0] + 24, tag_box[1] + 20], fill=(22, 163, 74))
    draw.text((tag_box[0] + 34, tag_box[1] + 7), tag_text, fill=(30, 64, 175), font=FONT_CAPTION)

    # 柔和分割线
    draw.line([(card_box[0], card_box[1] + 56), (card_box[2], card_box[1] + 56)], fill=(241, 245, 249), width=1)

def draw_floating_subtitle_bar(draw: ImageDraw.ImageDraw, text: str):
    # 底部高透白底磨砂胶囊 + 沉稳深色文字
    sub_w = 1560
    sub_h = 60
    sub_x = (WIDTH - sub_w) // 2
    sub_y = 900
    draw.rounded_rectangle([sub_x, sub_y, sub_x + sub_w, sub_y + sub_h], radius=30, fill=(255, 255, 255, 245), outline=(203, 213, 225), width=2)
    bbox = draw.textbbox((0, 0), text, font=FONT_SUBTITLE_BAR)
    tw = bbox[2] - bbox[0]
    draw.text((sub_x + (sub_w - tw) // 2, sub_y + 16), text, fill=(15, 23, 42), font=FONT_SUBTITLE_BAR)

# -------------------------------------------------------------
# 4. 各场景专属高品质舒服浅色排版
# -------------------------------------------------------------
def render_scene_content(draw: ImageDraw.ImageDraw, scene: dict, card_box: list, frame_idx: int):
    sid = scene["id"]
    cx1, cy1, cx2, cy2 = card_box
    content_y = cy1 + 70

    # 顶栏大标题区 (40px 粗深黑 + 21px 舒适冷灰)
    draw.text((cx1 + 40, content_y), scene["title"], fill=(15, 23, 42), font=FONT_TITLE)
    draw.text((cx1 + 42, content_y + 54), scene["subtitle"], fill=(100, 116, 139), font=FONT_SUBTITLE)
    draw.line([(cx1 + 40, content_y + 90), (cx2 - 40, content_y + 90)], fill=(241, 245, 249), width=2)
    
    body_y = content_y + 106

    if sid == 1:
        # 分镜1: 痛点暴击！(传统理财App盲盒 vs 自研实时神器解法)
        half_w = (cx2 - cx1 - 100) // 2
        # 左侧: 传统理财App痛点警示卡 (浅温润红底)
        warn_box = [cx1 + 40, body_y + 10, cx1 + 40 + half_w, body_y + 490]
        draw.rounded_rectangle(warn_box, radius=18, fill=(254, 242, 242), outline=(254, 202, 202), width=1)
        draw.text((warn_box[0] + 28, warn_box[1] + 24), "⚠️ 传统理财 App 痛点盲盒：", fill=(220, 38, 38), font=FONT_CARD_TITLE)
        
        warn_items = [
            ("盘中实时估值全面下架", "白天大盘暴跌暴涨，收益无法感知，两眼一抹黑"),
            ("收盘后被动开盲盒", "晚上8点基金公司才出净值，白白错过白天最佳减仓点"),
            ("纯债基金估值永远为 0", "传统软件纯债基金白天不动，无法应对债市剧烈波动"),
            ("记账无法知晓到手收益", "只能知道持有份额，今天实际赚亏几百块完全算不清"),
        ]
        for widx, (wtitle, wdesc) in enumerate(warn_items):
            wy = warn_box[1] + 75 + widx * 96
            draw.rounded_rectangle([warn_box[0] + 20, wy, warn_box[2] - 20, wy + 82], radius=10, fill=(255, 255, 255), outline=(254, 226, 226), width=1)
            draw.text((warn_box[0] + 36, wy + 14), f"✕  {wtitle}", fill=(220, 38, 38), font=FONT_CARD_TITLE)
            draw.text((warn_box[0] + 36, wy + 46), wdesc, fill=(100, 116, 139), font=FONT_CAPTION)

        # 右侧: 自研实时估值破局方案 (浅清爽蓝底)
        sol_box = [cx1 + 40 + half_w + 20, body_y + 10, cx2 - 40, body_y + 490]
        draw.rounded_rectangle(sol_box, radius=18, fill=(240, 249, 255), outline=(186, 230, 253), width=1)
        draw.text((sol_box[0] + 28, sol_box[1] + 24), "⚡ 自研实时穿透神器破局：", fill=(3, 105, 161), font=FONT_CARD_TITLE)
        
        # 实时大数字脉冲卡 (白底带高亮)
        pulse_box = [sol_box[0] + 24, sol_box[1] + 75, sol_box[2] - 24, sol_box[1] + 230]
        draw.rounded_rectangle(pulse_box, radius=14, fill=(255, 255, 255), outline=(191, 219, 254), width=1)
        draw.text((pulse_box[0] + 24, pulse_box[1] + 16), "全市场公募基金 · 盘中秒级加权实时计算中...", fill=(37, 99, 235), font=FONT_CAPTION)
        draw.text((pulse_box[0] + 24, pulse_box[1] + 46), "+2.85%", fill=(220, 38, 38), font=FONT_HERO)
        draw.text((pulse_box[0] + 24, pulse_box[1] + 112), "估算净值: 2.5500 (14:30 实时点)", fill=(15, 23, 42), font=FONT_CARD_TITLE)

        sol_items = [
            ("独家全天分时折线走势", "像股票看盘一样看基金日内走势脉搏"),
            ("穿透前十大重仓 + 纯债利率驱动", "股票秒级穿透，债券跟踪国债收益率"),
            ("输入支付宝金额，秒推今日盈亏", "今天赚亏多少钱，盘中实时给您算清每一分！"),
        ]
        for sidx, (stitle, sdesc) in enumerate(sol_items):
            sy = sol_box[1] + 245 + sidx * 75
            draw.rounded_rectangle([sol_box[0] + 24, sy, sol_box[2] - 24, sy + 64], radius=8, fill=(255, 255, 255), outline=(186, 230, 253), width=1)
            draw.text((sol_box[0] + 40, sy + 10), f"✓  {stitle}", fill=(16, 185, 129), font=FONT_BODY)
            draw.text((sol_box[0] + 40, sy + 34), sdesc, fill=(71, 85, 105), font=FONT_CAPTION)

    elif sid == 2:
        # 分镜2: 自选大盘库 (批量添加 + 拖拽排序 + 舒适红绿胶囊)
        tb = [cx1 + 40, body_y + 6, cx2 - 40, body_y + 64]
        draw.rounded_rectangle(tb, radius=12, fill=(248, 250, 252), outline=(226, 232, 240), width=1)
        draw.text((tb[0] + 24, tb[1] + 18), "自选基金监控大盘 (当前实时追踪 32 只标的)", fill=(15, 23, 42), font=FONT_BODY)
        
        draw.rounded_rectangle([tb[2] - 340, tb[1] + 10, tb[2] - 210, tb[3] - 10], radius=8, fill=(37, 99, 235))
        draw.text((tb[2] - 324, tb[1] + 15), "+ 批量导入", fill=(255, 255, 255), font=FONT_CAPTION)
        draw.rounded_rectangle([tb[2] - 190, tb[1] + 10, tb[2] - 90, tb[3] - 10], radius=8, fill=(255, 255, 255), outline=(203, 213, 225))
        draw.text((tb[2] - 175, tb[1] + 15), "拖拽排序", fill=(71, 85, 105), font=FONT_CAPTION)
        draw.rounded_rectangle([tb[2] - 70, tb[1] + 10, tb[2] - 15, tb[3] - 10], radius=8, fill=(255, 255, 255), outline=(203, 213, 225))
        draw.text((tb[2] - 56, tb[1] + 15), "刷新", fill=(71, 85, 105), font=FONT_CAPTION)

        items = [
            ("易方达消费行业股票", "110022", "主动股票", "2.5500", "+2.85%", "2.4793", "14:30", True),
            ("华夏国证半导体芯片ETF", "159995", "场内ETF", "1.1280", "+1.65%", "1.1097", "14:30", True),
            ("招商中证白酒指数分级", "161725", "指数型", "0.8920", "+2.12%", "0.8735", "14:30", True),
            ("富国产业债债券A", "100058", "债券-纯债", "1.2385", "-0.08%", "1.2395", "14:30", False),
            ("易方达蓝筹精选混合", "005827", "混合型", "1.8920", "+1.25%", "1.8686", "14:30", True),
        ]
        for idx, (fname, fcode, ftype, estnav, estpct, prevnav, qtime, isup) in enumerate(items):
            iy = body_y + 80 + idx * 72
            ibox = [cx1 + 40, iy, cx2 - 40, iy + 62]
            draw.rounded_rectangle(ibox, radius=10, fill=(255, 255, 255), outline=(226, 232, 240), width=1)
            
            # 拖拽把手
            hx, hy = ibox[0] + 16, iy + 22
            for dx in (0, 6):
                for dy in (0, 7, 14):
                    draw.ellipse([hx + dx, hy + dy, hx + dx + 3, hy + dy + 3], fill=(148, 163, 184))
            
            draw.text((ibox[0] + 45, iy + 11), fname, fill=(15, 23, 42), font=FONT_BODY)
            draw.text((ibox[0] + 45, iy + 36), f"{fcode} · 昨净 {prevnav} · 行情时间 {qtime}", fill=(100, 116, 139), font=FONT_CAPTION)
            
            # 分类标签
            tag_color = (224, 231, 255) if "指数" in ftype or "场内" in ftype else (254, 226, 226) if "主动" in ftype else (220, 252, 231)
            text_color = (67, 56, 202) if "指数" in ftype or "场内" in ftype else (185, 28, 28) if "主动" in ftype else (21, 128, 61)
            tbox = [ibox[0] + 440, iy + 14, ibox[0] + 540, iy + 44]
            draw.rounded_rectangle(tbox, radius=6, fill=tag_color)
            draw.text((tbox[0] + 14, tbox[1] + 6), ftype, fill=text_color, font=FONT_CAPTION)

            # 估算净值
            draw.text((ibox[2] - 300, iy + 14), estnav, fill=(220, 38, 38) if isup else (22, 163, 74), font=FONT_MEDIUM_NUM)
            
            # 涨跌幅胶囊徽章 (温润浅底红绿)
            bbox = [ibox[2] - 140, iy + 12, ibox[2] - 20, iy + 48]
            draw.rounded_rectangle(bbox, radius=6, fill=(254, 242, 242) if isup else (240, 253, 244), outline=(254, 202, 202) if isup else (187, 247, 208), width=1)
            draw.text((bbox[0] + 20, bbox[1] + 7), estpct, fill=(220, 38, 38) if isup else (22, 163, 74), font=FONT_BODY)

        tip_box = [cx1 + 40, body_y + 450, cx2 - 40, body_y + 510]
        draw.rounded_rectangle(tip_box, radius=12, fill=(240, 249, 255), outline=(186, 230, 253), width=1)
        draw.text((tip_box[0] + 28, tip_box[1] + 18), "⚡ 支持逗号/分号批量极速导入数十只基金代码，单轴吸附拖拽排序，全市场基金日内秒级刷新！", fill=(3, 105, 161), font=FONT_BODY)

    elif sid == 3:
        # 分镜3: 独家日内分时走势图 (脉冲心电图折线)
        info_w = 460
        ibox = [cx1 + 40, body_y + 6, cx1 + 40 + info_w, body_y + 490]
        draw.rounded_rectangle(ibox, radius=16, fill=(248, 250, 252), outline=(226, 232, 240), width=1)
        draw.text((ibox[0] + 24, ibox[1] + 22), "易方达消费行业股票 (110022)", fill=(15, 23, 42), font=FONT_CARD_TITLE)
        draw.text((ibox[0] + 24, ibox[1] + 68), "盘中实时估算净值 (14:30 连续竞价)", fill=(100, 116, 139), font=FONT_CAPTION)
        draw.text((ibox[0] + 24, ibox[1] + 96), "2.5500", fill=(220, 38, 38), font=FONT_HERO)
        
        draw.rounded_rectangle([ibox[0] + 24, ibox[1] + 170, ibox[0] + 160, ibox[1] + 215], radius=8, fill=(254, 242, 242), outline=(254, 202, 202), width=1)
        draw.text((ibox[0] + 40, ibox[1] + 178), "+2.85%", fill=(220, 38, 38), font=FONT_CARD_TITLE)

        draw.text((ibox[0] + 24, ibox[1] + 245), "昨日收盘净值: 2.4793 (基准锚点)", fill=(71, 85, 105), font=FONT_BODY)
        draw.text((ibox[0] + 24, ibox[1] + 285), "日内波动振幅: 3.12% (脉冲活跃)", fill=(71, 85, 105), font=FONT_BODY)
        draw.text((ibox[0] + 24, ibox[1] + 325), "重仓股票覆盖: 前十大重仓权重 62.4%", fill=(71, 85, 105), font=FONT_BODY)
        draw.text((ibox[0] + 24, ibox[1] + 365), "数据更新频率: 交易所直连每秒计算", fill=(71, 85, 105), font=FONT_BODY)
        draw.text((ibox[0] + 24, ibox[1] + 430), "⚡ 独家分时图: 捕捉下午跳水与拉升买卖点", fill=(37, 99, 235), font=FONT_BODY)

        # 右侧 ECharts 风格高保真分时走势图
        chart_box = [cx1 + 40 + info_w + 24, body_y + 6, cx2 - 40, body_y + 490]
        draw.rounded_rectangle(chart_box, radius=16, fill=(255, 255, 255), outline=(226, 232, 240), width=1)
        draw.text((chart_box[0] + 24, chart_box[1] + 20), "日内实时估值走势图 (240分钟全天心电图脉冲)", fill=(15, 23, 42), font=FONT_CARD_TITLE)

        gx1, gy1, gx2, gy2 = chart_box[0] + 60, chart_box[1] + 85, chart_box[2] - 40, chart_box[3] - 70
        draw.rectangle([gx1, gy1, gx2, gy2], outline=(241, 245, 249), width=1)
        for y_line in range(gy1, gy2, 60):
            draw.line([(gx1, y_line), (gx2, y_line)], fill=(241, 245, 249), width=1)
        mid_y = gy1 + (gy2 - gy1) // 2
        draw.line([(gx1, mid_y), (gx2, mid_y)], fill=(203, 213, 225), width=1)
        draw.text((gx1 + 10, mid_y - 20), "昨日收盘基准线 (2.4793 / 0.00%)", fill=(148, 163, 184), font=FONT_CAPTION)

        # 动态上扬珊瑚红折线走势
        curve_pts = []
        n_pts = 75
        for pidx in range(n_pts):
            px = gx1 + (gx2 - gx1) * pidx / (n_pts - 1)
            phase = frame_idx * 0.12
            trend = math.sin(pidx * 0.16 + phase) * 20 + (pidx / n_pts) * 95
            py = mid_y - trend + 15
            curve_pts.append((px, py))

        poly_pts = [(gx1, gy2)] + curve_pts + [(gx2, gy2)]
        draw.polygon(poly_pts, fill=(254, 242, 242, 130))
        for lidx in range(len(curve_pts) - 1):
            draw.line([curve_pts[lidx], curve_pts[lidx + 1]], fill=(220, 38, 38), width=3)

        # 终点脉冲发光球
        last_pt = curve_pts[-1]
        draw.ellipse([last_pt[0] - 8, last_pt[1] - 8, last_pt[0] + 8, last_pt[1] + 8], fill=(220, 38, 38))
        draw.ellipse([last_pt[0] - 14, last_pt[1] - 14, last_pt[0] + 14, last_pt[1] + 14], outline=(220, 38, 38, 120), width=2)
        
        tbox = [last_pt[0] - 190, last_pt[1] - 52, last_pt[0] - 12, last_pt[1] - 8]
        draw.rounded_rectangle(tbox, radius=6, fill=(15, 23, 42))
        draw.text((tbox[0] + 14, tbox[1] + 8), "实时估值: 2.5500 (+2.85%)", fill=(255, 255, 255), font=FONT_CAPTION)

        times = ["09:30 (开盘)", "10:30", "11:30 / 13:00 (午盘)", "14:00", "15:00 (收盘)"]
        for tidx, tm in enumerate(times):
            tx_label = gx1 + (gx2 - gx1) * tidx / (len(times) - 1) - 25
            draw.text((tx_label, gy2 + 12), tm, fill=(100, 116, 139), font=FONT_CAPTION)

    elif sid == 4:
        # 分镜4: 双核硬核穿透 (重仓股加权 + 纯债十年期国债模型)
        half_w = (cx2 - cx1 - 104) // 2
        # 左栏: 股票前十大重仓秒级穿透
        left_box = [cx1 + 40, body_y + 6, cx1 + 40 + half_w, body_y + 490]
        draw.rounded_rectangle(left_box, radius=16, fill=(248, 250, 252), outline=(226, 232, 240), width=1)
        draw.text((left_box[0] + 24, left_box[1] + 20), "股票型/混合型 · 穿透前十大重仓股票", fill=(15, 23, 42), font=FONT_CARD_TITLE)
        draw.text((left_box[0] + 24, left_box[1] + 62), "直连沪深交易所真实行情，按季报持仓占比精准加权运算：", fill=(100, 116, 139), font=FONT_CAPTION)
        
        stocks = [
            ("贵州茅台 (600519)", "9.85%", "+2.35%", 110),
            ("宁德时代 (300750)", "8.42%", "+3.10%", 95),
            ("腾讯控股 (00700)", "7.10%", "-0.45%", 80),
            ("美的集团 (000333)", "5.25%", "+1.25%", 60),
            ("五粮液 (000858)", "4.80%", "+2.15%", 54),
        ]
        for sidx, (sname, sw, spct, sbar_w) in enumerate(stocks):
            sy = left_box[1] + 100 + sidx * 72
            srow = [left_box[0] + 20, sy, left_box[2] - 20, sy + 62]
            draw.rounded_rectangle(srow, radius=8, fill=(255, 255, 255), outline=(226, 232, 240), width=1)
            draw.text((srow[0] + 16, sy + 11), sname, fill=(15, 23, 42), font=FONT_BODY)
            draw.text((srow[0] + 16, sy + 35), f"持仓占比 {sw}", fill=(100, 116, 139), font=FONT_CAPTION)
            draw.rounded_rectangle([srow[0] + 160, sy + 38, srow[0] + 160 + sbar_w, sy + 46], radius=4, fill=(37, 99, 235))
            is_up = "+" in spct
            draw.text((srow[2] - 120, sy + 18), spct, fill=(220, 38, 38) if is_up else (22, 163, 74), font=FONT_MEDIUM_NUM)

        # 右栏: 纯债基金国债利率驱动模型
        right_box = [cx1 + 40 + half_w + 24, body_y + 6, cx2 - 40, body_y + 490]
        draw.rounded_rectangle(right_box, radius=16, fill=(240, 249, 255), outline=(186, 230, 253), width=1)
        draw.text((right_box[0] + 24, right_box[1] + 20), "纯债基金 · 十年期国债收益率驱动模型", fill=(3, 105, 161), font=FONT_CARD_TITLE)
        draw.text((right_box[0] + 24, right_box[1] + 62), "银行间债券不公开秒级逐券行情，系统采用久期利率模型动态推演：", fill=(100, 116, 139), font=FONT_CAPTION)
        
        f_box = [right_box[0] + 20, right_box[1] + 100, right_box[2] - 20, right_box[1] + 235]
        draw.rounded_rectangle(f_box, radius=12, fill=(255, 255, 255), outline=(186, 230, 253), width=1)
        draw.text((f_box[0] + 20, f_box[1] + 18), "估值公式: ΔP ≈ - Duration × Δy (中债加权推导算法)", fill=(2, 132, 199), font=FONT_BODY)
        draw.text((f_box[0] + 20, f_box[1] + 54), "• 跟踪基准: 10 年期国债活跃收益率 (103.TY00Y)\n• 组合平均久期: 3.8 年  |  基准参考久期: 10.0 年\n• 自动识别定期报告平均久期，日内利率涨跌高频联动", fill=(71, 85, 105), font=FONT_BODY)

        b_tags = ["长债/短债智能模型分类", "支持人工组合久期校准", "告别传统纯债盘中恒定为零的尴尬"]
        for bidx, btag in enumerate(b_tags):
            by = right_box[1] + 260 + bidx * 60
            draw.rounded_rectangle([right_box[0] + 20, by, right_box[2] - 20, by + 50], radius=8, fill=(255, 255, 255), outline=(186, 230, 253), width=1)
            draw.text((right_box[0] + 36, by + 14), f"✓  {btag}", fill=(3, 105, 161), font=FONT_BODY)

    elif sid == 5:
        # 分镜5: 对标支付宝4大指标 · 实时推算今日到手收益
        col_w = 408
        cards = [
            ("今日收益 (盘中实时算清)", "+¥ 386.50", "日内涨幅: +1.93%", (220, 38, 38), (254, 242, 242)),
            ("持仓总市值 (动态现值)", "¥ 68,520.00", "昨日基准: ¥68,133.50", (15, 23, 42), (255, 255, 255)),
            ("累计持仓盈亏", "+¥ 4,280.00", "累计回报率: +6.67%", (220, 38, 38), (255, 255, 255)),
            ("昨日官方收益 (账面到账)", "+¥ 120.00", "基金公司官方净值清算", (15, 23, 42), (255, 255, 255)),
        ]
        for idx, (cname, cval, csub, ccol, cbg) in enumerate(cards):
            bx = cx1 + 40 + idx * (col_w + 34)
            bbox = [bx, body_y + 6, bx + col_w, body_y + 175]
            draw.rounded_rectangle(bbox, radius=14, fill=cbg, outline=(226, 232, 240), width=1)
            draw.text((bx + 24, bbox[1] + 18), cname, fill=(100, 116, 139), font=FONT_CAPTION)
            draw.text((bx + 24, bbox[1] + 52), cval, fill=ccol, font=FONT_BIG_NUM)
            draw.text((bx + 24, bbox[1] + 124), csub, fill=(100, 116, 139), font=FONT_CAPTION)

        abox = [cx1 + 40, body_y + 195, cx2 - 40, body_y + 490]
        draw.rounded_rectangle(abox, radius=16, fill=(248, 250, 252), outline=(226, 232, 240), width=1)
        draw.text((abox[0] + 28, abox[1] + 22), "录入对标支付宝 4 大核心指标 (双向联动自动推算):", fill=(15, 23, 42), font=FONT_CARD_TITLE)
        
        f_cols = [("持有金额", "¥ 20,000.00"), ("昨日收益", "+¥ 50.00"), ("持有收益", "+¥ 1,500.00"), ("持有收益率", "+8.11%")]
        for fidx, (fk, fv) in enumerate(f_cols):
            fx = abox[0] + 28 + fidx * 410
            fbox = [fx, abox[1] + 70, fx + 380, abox[1] + 165]
            draw.rounded_rectangle(fbox, radius=12, fill=(255, 255, 255), outline=(226, 232, 240), width=1)
            draw.text((fx + 20, fbox[1] + 16), fk, fill=(100, 116, 139), font=FONT_CAPTION)
            draw.text((fx + 20, fbox[1] + 46), fv, fill=(15, 23, 42), font=FONT_BIG_NUM)

        draw.text((abox[0] + 28, abox[1] + 200), "⚡ 只需输入持有金额或收益，自动推导持仓份额；盘中每分钟结合估值引擎推算今日到手到账金额！", fill=(37, 99, 235), font=FONT_BODY)
        draw.text((abox[0] + 28, abox[1] + 236), "💡 零门槛记账，支持未来支付宝截图 OCR 自动扫描一键导入，再也不用手动按计算器！", fill=(100, 116, 139), font=FONT_BODY)

    elif sid == 6:
        # 分镜6: 响应式多端自适应 + 全网免费开源
        draw.text((cx1 + 40, body_y + 6), "全终端深度自适应，手机与电脑始终呈现最高清晰度与触感：", fill=(71, 85, 105), font=FONT_BODY)
        
        # 手机模型 (卡片流)
        phone_box = [cx1 + 60, body_y + 45, cx1 + 540, body_y + 490]
        draw.rounded_rectangle(phone_box, radius=24, fill=(15, 23, 42), outline=(51, 65, 85), width=2)
        draw.rounded_rectangle([phone_box[0] + 10, phone_box[1] + 14, phone_box[2] - 10, phone_box[3] - 14], radius=16, fill=(248, 250, 252))
        draw.text((phone_box[0] + 24, phone_box[1] + 22), "自选基金  |  持仓实时估值", fill=(15, 23, 42), font=FONT_CAPTION)
        
        m_funds = [
            ("易方达消费行业", "+¥260.00", "+2.85%", "¥20,000", True),
            ("华夏半导体芯片", "+¥126.50", "+1.65%", "¥15,000", True),
            ("招商中证白酒", "+¥180.00", "+2.12%", "¥15,000", True),
            ("富国产业债A", "-¥12.00", "-0.08%", "¥15,000", False),
        ]
        for midx, (mname, mincome, mpct, mamt, misup) in enumerate(m_funds):
            my = phone_box[1] + 50 + midx * 82
            mbox = [phone_box[0] + 16, my, phone_box[2] - 16, my + 72]
            draw.rounded_rectangle(mbox, radius=10, fill=(255, 255, 255), outline=(226, 232, 240), width=1)
            draw.text((mbox[0] + 14, my + 10), mname, fill=(15, 23, 42), font=FONT_CAPTION)
            draw.text((mbox[0] + 14, my + 38), f"持有 {mamt}", fill=(100, 116, 139), font=FONT_CAPTION)
            draw.text((mbox[2] - 140, my + 10), mincome, fill=(220, 38, 38) if misup else (22, 163, 74), font=FONT_MEDIUM_NUM)
            draw.text((mbox[2] - 110, my + 40), mpct, fill=(220, 38, 38) if misup else (22, 163, 74), font=FONT_CAPTION)

        draw.text((phone_box[0] + 20, phone_box[3] - 34), "⚡ 手机端独立吸顶导航 · 绝不挤压横移穿框", fill=(37, 99, 235), font=FONT_CAPTION)

        # PC 端宽屏专业大盘
        pc_box = [cx1 + 570, body_y + 45, cx2 - 40, body_y + 360]
        draw.rounded_rectangle(pc_box, radius=16, fill=(255, 255, 255), outline=(226, 232, 240), width=1)
        draw.rounded_rectangle([pc_box[0], pc_box[1], pc_box[2], pc_box[1] + 46], radius=14, fill=(241, 245, 249))
        draw.text((pc_box[0] + 24, pc_box[1] + 13), "基金名称 / 代码            持有金额        昨日收益      今日收益      最新累计盈亏", fill=(71, 85, 105), font=FONT_BODY)
        
        rows = [
            ("易方达消费行业 (110022)", "¥ 20,000.00", "+¥ 50.00", "+¥ 260.00 (+2.85%)", "+¥ 1,760.00"),
            ("华夏半导体芯片 (159995)", "¥ 15,000.00", "-¥ 30.00", "+¥ 126.50 (+1.65%)", "+¥ 980.00"),
            ("招商中证白酒分 (161725)", "¥ 15,000.00", "+¥ 40.00", "+¥ 180.00 (+2.12%)", "+¥ 1,420.00"),
            ("富国产业债债券 (100058)", "¥ 15,000.00", "+¥ 10.00", "-¥ 12.00 (-0.08%)", "+¥ 350.00"),
        ]
        for ridx, (rname, ramt, ryest, rtoday, rtotal) in enumerate(rows):
            ry = pc_box[1] + 54 + ridx * 62
            draw.text((pc_box[0] + 24, ry + 12), f"{rname}    {ramt}    {ryest}    {rtoday}    {rtotal}", fill=(15, 23, 42), font=FONT_BODY)
            draw.line([(pc_box[0] + 14, ry + 52), (pc_box[2] - 14, ry + 52)], fill=(241, 245, 249), width=1)

        # 终极开源号召横幅
        call_box = [cx1 + 570, body_y + 380, cx2 - 40, body_y + 490]
        draw.rounded_rectangle(call_box, radius=16, fill=(37, 99, 235), outline=(29, 78, 216), width=1)
        draw.text((call_box[0] + 36, call_box[1] + 22), "在线体验与完整源码：请查看【B站置顶评论与视频简介】", fill=(255, 255, 255), font=FONT_CARD_TITLE)
        draw.text((call_box[0] + 36, call_box[1] + 64), "★ 代码完全免费开源 · 求点赞、求投币、求收藏一键三连支持！", fill=(219, 234, 254), font=FONT_BODY)

# -------------------------------------------------------------
# 5. 视频构建流水线执行
# -------------------------------------------------------------
async def build_pipeline():
    print("=====================================================")
    print("🚀 启动 B站 1080P 高清吸睛视频全自动渲染流水线 (舒适浅色主题版)")
    print("=====================================================")

    print("\n[1/5] 正在生成微软超拟真活力男声 (zh-CN-YunxiNeural, rate=+22%)...")
    scene_audios = []
    scene_durations = []
    
    for sc in SCENES:
        sid = sc["id"]
        mp3_path = TEMP_DIR / f"voice_{sid}.mp3"
        await generate_speech_file(sc["tts_text"], mp3_path)
        dur = get_audio_duration(mp3_path)
        dur += 0.2
        sc["duration"] = dur
        sc["frames"] = int(dur * FPS)
        scene_audios.append(mp3_path)
        scene_durations.append(dur)
        print(f"  • 分镜 {sid}: 时长 {dur:.2f} 秒 ({sc['frames']} 帧) -> {sc['title']}")

    total_duration = sum(scene_durations)
    total_frames = sum(sc["frames"] for sc in SCENES)
    print(f"\n📊 视频总时长: {total_duration:.2f} 秒 (共 {total_frames} 帧) - 黄金完播率节奏！")

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

    print("\n[4/5] 正在渲染高级舒服浅色质感帧并压制成片...")
    card_box = [50, 40, WIDTH - 50, 880]

    for sc in SCENES:
        sid = sc["id"]
        frames_count = sc["frames"]
        sub_text = sc["subtitle_text"]
        tag_text = sc["tag"]

        for f_in_scene in range(frames_count):
            img = Image.new("RGB", (WIDTH, HEIGHT), (248, 250, 252))
            draw = ImageDraw.Draw(img)

            draw_gradient_background(draw, f_in_scene)
            draw_main_window(draw, card_box, tag_text)
            render_scene_content(draw, sc, card_box, f_in_scene)
            draw_floating_subtitle_bar(draw, sub_text)

            pipe.stdin.write(img.tobytes())

    pipe.stdin.close()
    pipe.wait()
    print("✅ 视频渲染压制全量完成！")

    print("\n[5/5] 更新 B站 专属爆款宣发物料指南...")
    generate_promo_kit(total_duration)
    print("✅ 宣发物料已生成！")

def generate_promo_kit(duration: float):
    kit_content = f"""# 基金实时估值系统 · B站专属流量爆款宣发全套物料 (高级舒适浅色主题版)

> **视频基本信息**：
> - 视频文件：`media_kit/fund_valuation_promo.mp4`
> - 画质规格：1080P 60FPS / 30FPS 超清标准，H.264 + AAC
> - 视频时长：约 {duration:.1f} 秒（黄金完播率 30~45s 区间）
> - 视觉主题：高级舒适浅色金融风 (Clean Light Finance)，清爽透气、温润耐看

---

## 一、 推荐爆款标题矩阵（精选 3 组，建议直接复制）

### 方案 A（🔥 强烈推荐 · 痛点直击 + 悬念反转型，点击率 CTR 极高）
**【全网首发】天天基金估值被下线后，我怒花两周手搓了一个实时估值神器！分时走势图+重仓穿透，源码全开源！**

### 方案 B（⚡ 科技数码 / 独立开发极客风，完播率高）
**下午2点半别再开盲盒了！自研公募基金盘中秒级实时估值系统，连日内分时心电图都画出来了！**

### 方案 C（📈 搞钱理财 / 实用干货型）
**基金盘中到底亏了多少钱？手把手教你秒算今日到手收益，股票穿透+纯债久期模型，附在线体验与源码！**

---

## 二、 视频简介模板（针对 B站 搜索算法 SEO 优化）

```text
天天基金和支付宝盘中估值全面下架后，白天大盘剧烈波动，根本不知道手里基金赚亏几何，只能晚上 8 点等基金公司公布净值开盲盒？

受够了收盘挨打的痛苦，我花了两周自研开发了这套【基金实时估值系统】！
不仅秒级高频穿透，甚至把日内全天 240 点的分时走势图与脉冲波形都画出来了！

🔥 核心硬核功能：
1. 【独家日内分时走势图】：像看股票分时线一样看基金盘中走势，捕捉下午跳水与拉升买卖点！
2. 【双核高频穿透引擎】：
   - 股票型/混合型：秒级直连交易所行情，按季报前十大重仓逐只加权穿透；
   - 纯债基金：接入十年期国债活跃收益率 (103.TY00Y) 与组合久期波动模型，彻底告别恒定为零！
3. 【对标支付宝4大指标】：输入持有金额，盘中实时推算今天实收到手赚亏多少钱（如 +¥386.50）！
4. 【自选大盘毫秒级追踪】：支持逗号批量导入、单轴吸附拖拽排序、红绿涨跌药丸一览无余。
5. 【全终端深度自适应】：手机端吸顶卡片流 + 电脑端专业高密度大盘。

📌 在线体验与完整源代码：
体验地址与 GitHub 仓库源码已放在【置顶评论】中，开箱即用，支持一键 Docker 私有化自建！

如果本期视频和工具对你有帮助，请务必【点赞、投币、收藏】一键三连支持一下独立开发者，大家的鼓励就是持续更新最大的动力！感谢大家！
```

---

## 三、 推荐热门标签 (Tags，直接复制填入 B站 标签栏)
`基金` `基金实时估值` `理财` `支付宝基金` `天天基金` `股票` `投资理财` `独立开发` `开源项目` `程序员` `Spring Boot` `React`

---

## 四、 爆款置顶评论模板（高转化率引导）

```text
各位基民与极客小伙伴大家好！感谢大家观看本期视频 🚀

📌 在线体验与开源地址已整理好：
👉 在线体验网址：http://trythis.pw
👉 GitHub 完整开源项目：https://github.com/duyuanfu/fund-valuation

💡 常见问题与功能答疑：
1. 【如何使用】：支持输入基金代码快速添加，也可以用逗号批量导入；输入金额即可实时测算今日收益；
2. 【数据来源】：直连交易所公开行情，季报前十大重仓逐只加权，纯债基金依托国债利率久期模型推算；
3. 【如何私有化自建】：项目已提供一键 Docker Compose 部署脚本，单机 800MB 内存即可轻量运行。

大家平时下午看盘最常关注哪只基金？欢迎在评论区留言交流！
觉得项目不错的小伙伴，求大佬们顺手点个【一键三连】支持一下开源作者，非常感谢！❤️
```
"""
    with open(PROMO_KIT_PATH, "w", encoding="utf-8") as f:
        f.write(kit_content)

if __name__ == "__main__":
    asyncio.run(build_pipeline())
