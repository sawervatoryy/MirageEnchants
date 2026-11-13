# MirageEnchants
**MirageStudio** — Автор: **sawervatoryy** — https://discord.gg/mQbZT3x3Q

Кастомные зачарования в стиле HolyWorld + редактор ванильных зачарований. Paper 1.21.4, Java 21.

## Сборка
`mvn clean package` (или Maven в IntelliJ) → `target/MirageEnchants-<версия>.jar` → в `plugins/`.
Soft-зависимости: Vault + Essentials (или любой плагин валюты с Vault) — только для «Богача».

## Команды и права
| Команда | Описание | Право |
|---|---|---|
| `/mirage list` | список зачарований | mirageenchants.admin |
| `/mirage book <игрок> <чар> [ур] [кол-во]` | выдать книгу | mirageenchants.admin |
| `/mirage add <чар> [ур] [force]` | наложить на предмет в руке | mirageenchants.admin |
| `/mirage remove <чар>` | снять с предмета в руке | mirageenchants.admin |
| `/mirage reload` | перезагрузка конфигов | mirageenchants.admin |
| `/mirage about` | о плагине | — |
| `/enchedit` | редактор зачарований | mirageenchants.editor |
| `/setfilter [add/remove/clear/list]` | фильтр предметов | mirageenchants.filter |

Редактор: ЛКМ +1, ПКМ −1, Shift+ЛКМ +10, Shift+ПКМ −10, Q — максимум, F — убрать. Ванильные до 255.
Точило: ПКМ по точилу (присед + ПКМ — ванильное).

## Конфиги
- `config.yml` — общие настройки (лор, наковальня, стол, прочность/починка, точило, редактор, экономика).
- `enchants.yml` — каждое зачарование: enabled, название, max-level, applies-to, шансы на столе, наковальня, settings (число или «уровень: значение»).
- `messages.yml` — все тексты.

## Примечания
- Кастомные чары хранятся в PDC предмета (`mirageenchants:e_<id>`) + строка лора.
- Броня: Прочность = 47 + 10×ур. %, Починка на броню запрещена, ремонт брони в наковальне и /fix блокируются.
- «Фармер»: LINEAR — ур.1 ×2, ур.2 ×4, ур.3 ×6 (режим EXPONENTIAL в enchants.yml даёт ×2, ×4, ×8).
