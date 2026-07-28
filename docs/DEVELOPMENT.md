# OneLab development rules

This document defines where code belongs and the safety rules for adding features.
It applies to production sources under `app/src/main`.

## Package layout

| Location | Responsibility |
| --- | --- |
| `io.github.pigerzhu.onelab.MainActivity` | Activity lifecycle, page navigation, transitions, and feature composition only. |
| `io.github.pigerzhu.onelab.*Screen` | One user-facing feature or one tightly related feature group. Owns its cards, dialogs, UI state, and event handling. |
| `io.github.pigerzhu.onelab.*Presenter` | Stateful Android platform sessions that are more complex than settings I/O, such as a secondary-display presentation. |
| `io.github.pigerzhu.onelab.system` | Privileged or device-specific I/O. Shell commands, Binder/service calls, Settings access wrappers, and Samsung service clients live here. No view construction. |
| `io.github.pigerzhu.onelab.contract` | Setting keys and data contracts shared between the app UI and hook processes. No Android component or I/O code. |
| `io.github.pigerzhu.onelab.hook` | LSPosed entry points and hooks. Hook code must not depend on Activity or Screen classes. |
| `io.github.pigerzhu.onelab.ui` | Reusable visual components and theme primitives. No feature settings keys or privileged operations. |
| `app/src/main/res` | Android resources. Shared icons and framework-required strings belong here. |
| `tools` | Repeatable repository maintenance scripts. Generated files must name their generator. |
| `analysis` and `apks` | Reverse-engineering evidence only. Production code must not load files from these directories. |

Do not create generic `Utils`, `Manager`, or `Helper` classes. Name a class after the
specific responsibility it owns. A new feature normally starts as `FeatureScreen`; add a
`system/FeatureClient` only when privileged I/O would otherwise be mixed into the UI.

## Dependency direction

Normal app code follows this direction:

```text
MainActivity -> Screen/Presenter -> system client
                         |
                         +-> ui components
```

Hook code is a separate runtime boundary:

```text
hook.Entry -> feature hooks -> HookConstants/HookUtils
```

- UI and hooks read shared setting keys from `contract/SettingsKeys`.
- Hooks must not call `Shell`, construct UI, or hold Activity references.
- Screens may configure hooks through Settings, but never call hook classes directly.
- `MainActivity` must not contain feature-specific Settings keys or shell commands.

## File and class rules

- One top-level production class per Java file.
- A Screen owns one page or a small feature group. Split unrelated cards into separate Screens.
- Prefer package-private classes and methods until another package genuinely needs the API.
- At 400 lines, review a file for mixed responsibilities. Above 600 lines, split it before adding another feature.
- Keep constants beside their owner. Put cross-process setting keys in `contract/SettingsKeys`.
- Comments explain device behavior, invariants, or reverse-engineered contracts, not obvious Java statements.

## Privileged operation rules

- Opening a page must not request root. Root is allowed only after an explicit user action.
- Never run `su`, Binder I/O, package scans, or device-service calls on the main thread.
- Preserve the previous value before changing experimental firmware or service settings.
- Every risky setting needs a bounded input range and a recovery path.
- A success message requires write-back verification when the target can silently reject writes.
- On failure, restore the visible control to the last confirmed state.
- Shell values must be quoted; do not concatenate untrusted text into a root command.
- Installation and package-component commands must target user 0 unless another user was explicitly requested.

## Hook safety rules

- Avoid broad method enumeration when a stable class and method are known.
- Never perform Settings or disk reads in a hot WindowManager/SystemUI method. Observe once, cache, and update the cache from a `ContentObserver`.
- Fail open: if parsing or reflection fails, preserve Samsung's original behavior.
- Scope each hook to the smallest required package/process.
- Do not block Binder, render, input, or animation threads.
- Log installation failures once; do not emit per-frame or per-call logs.

## UI rules

- Use `Ui` and existing Material components before adding a new visual primitive.
- Entry cards navigate; they do not also expose detailed controls.
- A control must reflect the last confirmed system state, not merely the requested state.
- Controls that write continuously must defer the write until interaction ends unless live updates are essential.
- New nested pages must set `MainActivity.nestedBackAction` to their owning parent page.

## Verification and commits

Before committing a behavioral change:

1. Run `git diff --check`.
2. Build `assembleDebug`.
3. Install with `adb install --user 0 -r` when a phone is connected.
4. Confirm installation did not activate an experimental setting by itself.
5. Review the final diff for unrelated generated or analysis files.

Keep commits limited to one coherent behavior or refactor. A structural refactor must preserve
settings keys, defaults, hook scope, and user-visible behavior unless the commit explicitly says
otherwise.

Keep public documentation focused on stable architecture, supported behavior, and
reproducible build or contribution instructions.
