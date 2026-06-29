# minecraft-plugin-menu

เมนู **ต่อผู้เล่นแต่ละคน** ในเกม — คำสั่ง `/menu` (alias `settings`/`options`/`prefs`) เปิดหน้าต่างฟอร์มขึ้นมาให้ผู้เล่นปรับค่าของตัวเอง ชื่อ plugin ที่โชว์ใน `/pl` = **`Menu`**

plugin ตัวนี้ **ไม่มี state / ไม่มีตารางของตัวเอง** — เป็นแค่ front-end:
- อ่านรายการ setting จาก `MenuRegistry` ของ core (feature plugin เป็นคน register setting ของตัวเอง)
- อ่าน/เขียนค่าต่อผู้เล่นผ่าน `PlayerPreferenceService` ของ core (เก็บลง DB กลาง ตาราง `setting_values`)

depend on `minecraft-plugin-core` แบบ `compileOnly` + `depend: [Core]` คุยผ่าน `CoreApi` ตอน runtime (ดู [CLAUDE.md](../CLAUDE.md))

## UI — Paper native Dialog API

ใช้ **Dialog API ของ Paper** (`io.papermc.paper.dialog.*`) render เป็นฟอร์มจริง ไม่ใช่ chest GUI hack — แต่ละ `MenuItem` map เป็น input ตามชนิด:

| `MenuItemType` | Dialog input | ค่าที่อ่านกลับ |
|---|---|---|
| `TOGGLE` | `BooleanDialogInput` | `DialogResponseView.getBoolean` |
| `CHOICE` | `SingleOptionDialogInput` (dropdown) | `getText` (= option value) |
| `NUMBER` | `NumberRangeDialogInput` (slider) | `getFloat` |
| `TEXT` | `TextDialogInput` | `getText` |

> Dialog input key ใช้เป็นชื่อ command-macro (อนุญาตแค่ `[A-Za-z0-9_]`) — setting key มีจุด ฉะนั้น `MenuDialog` ใช้ key แบบ positional (`s0`, `s1`, …) แล้ว map กลับเป็น setting key จริงตอนเซฟ

**หน้าเดียวรวมทุก option** — `/menu` เปิด dialog เดียว แสดงทุก `MenuItem` ที่ register ไว้เป็น input pre-fill ค่าปัจจุบันของผู้เล่น แล้วมี 2 ปุ่มข้างล่าง:
- **Save** — เขียนทุก input กลับผ่าน `PlayerPreferenceService.set(...)` (อัปเดต cache ทันที → effect realtime)
- **Cancel** — ปิดเฉย ๆ ไม่ save (เหมือนกด Esc)

## เพิ่ม setting ใหม่ (ทำที่ feature plugin ไม่ต้องแตะ plugin นี้)

```java
// onEnable ของ feature plugin
CoreApi.menu(getServer()).ifPresent(reg -> reg.register(
        MenuItem.choice("healthbar.display", "Health bar",
                "Health bar display", "How damaged entities' health shows to you",
                List.of(new MenuItem.Option("bar", "Bar"),
                        new MenuItem.Option("number", "Number")),
                "bar")));
```

`/menu` โชว์ item ใหม่ให้อัตโนมัติ (อ่าน registry ตอนเปิดเมนู) เรียงตามลำดับที่ register — ปัจจุบันมีผู้ใช้แล้ว: **Money** (show on `/money top`) และ **Health bar** (เปิด/ปิด + bar/number)

> `category` (arg ที่ 2 ของ `MenuItem`) ตอนนี้ยังไม่ถูกใช้จัดกลุ่มใน UI (ทุก option อยู่หน้าเดียว) — เก็บไว้เผื่อแบ่งหมวดภายหลัง

## สถานะ

- ✅ `/menu` เปิด Dialog หน้าเดียวรวมทุก option จาก registry + Save/Cancel, เซฟผ่าน `PlayerPreferenceService`
- ⏳ ยังไม่แบ่งหมวด/แบ่งหน้า — ถ้า option เยอะขึ้นมากค่อยเพิ่ม paging หรือจัดกลุ่มตาม `category`

## Build

```
./gradlew :minecraft-plugin-menu:build
```
