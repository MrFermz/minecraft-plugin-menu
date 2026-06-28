# minecraft-plugin-setting

UI ตั้งค่า **ต่อผู้เล่นแต่ละคน** ในเกม — คำสั่ง `/setting` (alias `settings`/`options`/`prefs`) เปิดหน้าต่างฟอร์มขึ้นมาให้ผู้เล่นปรับค่าของตัวเอง ชื่อ plugin ที่โชว์ใน `/pl` = **`Settings`**

plugin ตัวนี้ **ไม่มี state / ไม่มีตารางของตัวเอง** — เป็นแค่ front-end:
- อ่านรายการ setting จาก `SettingsRegistry` ของ core (feature plugin เป็นคน register setting ของตัวเอง)
- อ่าน/เขียนค่าต่อผู้เล่นผ่าน `PlayerPreferenceService` ของ core (เก็บลง DB กลาง ตาราง `setting_values`)

depend on `minecraft-plugin-core` แบบ `compileOnly` + `depend: [Core]` คุยผ่าน `CoreApi` ตอน runtime (ดู [CLAUDE.md](../CLAUDE.md))

## UI — Paper native Dialog API

ใช้ **Dialog API ของ Paper** (`io.papermc.paper.dialog.*`) render เป็นฟอร์มจริง ไม่ใช่ chest GUI hack — แต่ละ `SettingDefinition` map เป็น input ตามชนิด:

| `SettingType` | Dialog input | ค่าที่อ่านกลับ |
|---|---|---|
| `TOGGLE` | `BooleanDialogInput` | `DialogResponseView.getBoolean` |
| `CHOICE` | `SingleOptionDialogInput` (dropdown) | `getText` (= option value) |
| `NUMBER` | `NumberRangeDialogInput` (slider) | `getFloat` |
| `TEXT` | `TextDialogInput` | `getText` |

หน้าจอ pre-fill ค่าปัจจุบันของผู้เล่น, ปุ่ม **Save** เขียนทุก input กลับผ่าน `PlayerPreferenceService.set(...)` (อัปเดต cache ทันที → effect realtime), กด Esc = ยกเลิก

## เพิ่ม setting ใหม่ (ทำที่ feature plugin ไม่ต้องแตะ plugin นี้)

```java
// onEnable ของ feature plugin
CoreApi.settings(getServer()).ifPresent(reg -> reg.register(
        SettingDefinition.choice("healthbar.display", "Healthbar",
                "Health bar display", "How damaged entities' health shows to you",
                List.of(new SettingDefinition.Option("bar", "Bar"),
                        new SettingDefinition.Option("number", "Number (current/total)")),
                "bar")));
```

`/setting` จะโชว์ setting ใหม่ให้อัตโนมัติ (อ่าน registry ตอนเปิดเมนู) — ปัจจุบันมีผู้ใช้แล้ว: `Money` (show on `/money top`) และ `Healthbar` (bar/number)

## สถานะ

- ✅ `/setting` เปิด Dialog จาก registry, เซฟผ่าน `PlayerPreferenceService`
- ⏳ ยังเป็นหน้าเดียวรวมทุก setting (ยังไม่แบ่งหน้า/หมวดเป็น dialog list) — เพิ่มภายหลังได้เมื่อ setting เยอะขึ้น

## Build

```
./gradlew :minecraft-plugin-setting:build
```
