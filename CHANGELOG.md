# Changelog

# Unreleased

* **Breaking:** Moved Bukkit and Minestom platform classes that collided on the same package name — making it impossible to depend on both platforms in one project — into platform-specific subpackages (#782):
  * Bukkit: `me.devnatan.inventoryframework.{View,ViewFrame,IFInventoryListener}` → `me.devnatan.inventoryframework.bukkit.{View,ViewFrame,IFInventoryListener}`; `me.devnatan.inventoryframework.context.{Context,CloseContext,OpenContext,RenderContext,SlotClickContext,SlotContext,SlotRenderContext}` → `me.devnatan.inventoryframework.bukkit.context.*`.
  * Minestom: same classes move from the identical old paths to `me.devnatan.inventoryframework.minestom` and `me.devnatan.inventoryframework.minestom.context` respectively.
  * The Paper artifact re-exports Bukkit's classes unchanged, so it picks up the new `me.devnatan.inventoryframework.bukkit` package automatically.
  * Update your imports accordingly; a simple find-and-replace of the old package prefix with the platform-specific one covers most cases.

# 3.2.0 (12-05-2024)

* Minecraft v1.21.3 support. Thanks to @nicolube (#688)

# 3.0.12 (12-04-2024)

* Prevent catastrophic when item render or click fails. Thanks to @nicolube (#686)
* Add support for Paper 1.21+ to AnvilInput. Thanks to @mfnalex (#684)

# 3.0.11 (09-11-2023)

* Fix scheduled update job cancellation check
* Context invalidation viewers size check

# 3.0.8 (11-28-2023)

* Interaction delay intercepting re-propagated interactions (#543)

# 3.0.7 (11-28-2023)

* Solve issues with click handlers not working in pagination by calling component interaction handler in core interceptors (#542)

# 3.0.6 (11-27-2023)

* New `createEndlessContext` and `openEndless` to create shared views with multiple players that are never invalidated (#538)
* Re-propagate click interaction in pagination child - now `cancelOnClick()` works correctly in pagination items (#539)
* Some components disappearing on global context update (#541)

# 3.0.5 (11-26-2023)

* Experimental `onViewerAdded` and `onViewerRemoved` to manage viewers in shared contexts (#516)
* New `openActive(...)` to allow players "join" an already created context (#516)
* Now ComponentBuilder's `closeOnClick()` closes only for the player who clicked, not for everyone in Shared Context (#516)
* `type(ViewType.ANVIL)` is now automatically set when Anvil Input feature is applied (#523)
* Do not fill all available slots when availableSlot() is used (#533)
* Fix "Pagination state can only be registered by RootView" (#536)

# 3.0.4 (11-20-2023)

* Scheduled update now checks if a context is active before updating it (#517)
* Now Anvil Input feature ingredient display name is updated when state value changes (#494)
* Modify ComponentBuilder's `updateOnStateChange` to prohibit no-parameter usage (#520)
* Components now use unified update mechanism in `show()` and `hide()` to ensure consistency and behavior in relation to other components (#491)

# 3.0.3 (11-19-2023)

* Minecraft 1.20.2 support (#514)
* Close context cancellation not working (#515)
* Decouple state access from PlatformView to allow State Management API usage anywhere (#496)
* ViewConfig `with(Modifier)` is now `use(Modifier)` (#513)
* Bump junit from 5.10.0 to 5.10.1 (#502)
* Bump org.jetbrains:annotations from 24.0.1 to 24.1.0 (#512)

# 3.0.2 (10-05-2023)

* Kotlin stdlib from inventory-framework-platform-paper module runtime dependencies (#495)
* Fix scheduled update start interceptor trying to restart on context close and throwing an exception (#497)

# 3.0.1 (10-04-2023)

* Lock child interactions while pagination page is changing to prevent items be removed while asynchronous pagination is reloading (#488)
* Option to enable data transitivity from one view to another automatically (#493)
* Remove multiple layoutSlot with same character restriction, now multiple `layoutSlot` with the same character can be used (#489)

# 3.0.0 (09-21-2023)

* Refs API feature to create a direct component instance reference (#476)
* Anvil Input feature that allows to get the text field value from a anvil (#481)
* Context back with initialData parameter (#482)
* Fix inconsistent page index in pagination page switch handler (#478)
* OpenContext and CloseContext now implements Bukkit platform Context (#479)
* Fix MC 1.8 inventory close handling exploit with F6 (Start Streaming) key (#480)
* New experimental Component hide() and show() also forceUpdate() was moved from Pagination API to Component API (#476)
* IN-GAME library conflict warning removed (#484)
* Added an option to disable bStats metrics (#485)

# 3.0.0-rc.3 (09-13-2023)

Please see [release notes](https://github.com/DevNatan/inventory-framework/releases/tag/v3.0.0-rc.3) to detailed changelog.

# 3.0.0-rc.2 (08-15-2023)

* New `openForPlayer()` and `openForEveryone()` with initial data parameter like in ViewFrame (#421)
* Minecraft 1.19 and 1.20 support in dynamic title update (#423)
* New `updateTitleForPlayer(title, player)` to update title for a specific player (#424)
* Changed how updates and component rendering are handled fixing some issues with Pagination and other components (#425)
* Asynchronous Pagination *(experimental)* (#432)
* Internal state and component management related methods were marked as Internal API
* Project module names changed now every module has "inventory-framework-" prefix
* Removed kotlin-dsl and feature-event-bus modules
* [Pagination on Wiki](https://github.com/DevNatan/inventory-framework/wiki/Pagination) had its structure reformulated to simplify the reading, go more straight to the point, add some missing topics to explain better about pagination with data source and dynamic items
* Page "Inventory Title Programmatic Update" on Wiki reworked and renamed to [Dynamic Title Update](https://github.com/DevNatan/inventory-framework/wiki/dynamic-title-update)

# 3.0.0-rc.1 (08-07-2023)

* Null `getPlayer()` in RenderContext (#414)
* Error when a empty pagination source is provided (#415)
* Dynamically rendered items in pagination being positioned in the wrong place (#416)
* New `ViewFrame.open(view, player, data)` overload (#417)
* New `item.updateOnClick()` to trigger context update on item click (#418)
* `initialState()` is now mutable so its value can be changed now (#413)
* Bump junit from 5.9.3 to 5.10.0 (#406)
* Fix relocation warn by checking for InventoryFramework class in runtime (#412)

# 3.0.0-beta (08-04-2023)

* Inventory Framework is now published to Maven Central (#408)
* Shared Contextx preview (#409)

# 3.0.0-alpha (08-01-2023)

* Item render and update handler shortcuts (#274)
* Wrong JDK version (#386)
* Lombok removed (#391)
* Allow change renderization slot target while item renderization phase (#395)
* New `unsetSlot` (#395)
* Fix layout component registration order to ensure it works with PaginationState (#399)
* New item click handler `onClick(Runnable)` overload (#400)
* Code and Wiki documentation improvement

# 3.0.0-EAP (06-07-2023)

Please refer to [Migrating to version 3 on Wiki](https://github.com/DevNatan/inventory-framework/wiki/Migrating-to-version-3) for migration details.

# 2.5.4-rc.1 (10-09-2022)

* New context history feature (#120)
* Fixed context data inheritance (#219)
* Fixed NullPointerException in context paginator (#224)
* Update IF-specific bStats charts (#227)
* Fixed "Update job not started" (#232)
* Bump com.diffplug.spotless from 6.9.1 to 6.10.0 (#214)
* New Event Bus feature (#234)
* Fixed page switch issues due to dynamic pagination source (#233, #238)
* Fixed wrong update interval when using schedule update with Duration as parameter (#236)
* Try to fix ConcurrentModificationException on open (#237)
* Fixed fallback navigation item retrieval compatibility (#228)
* Allows nullable values on pagination sources (#235)
* Fixed pagination context `getSlot()` returning index instead of current slot (#235)
* Fixed context `getCurrentItem` always returning null (#235)
* New `onSlotRender` handler prototype (#235)
* Now `clear()` supports immediate clear when context is on entity container (#235)
* Provide unique element index for pagination (#239)

# 2.5.4-beta (23-09-2022)
* Auto slot fill and layout can now be used on regular views and paginated views (#174)
* Updated Gradle to v7.5.1 (#189)
* Remove nextTick from view open, close and updateSlot call (#193)
* Update plugin com.diffplug.spotless to v6.9.1 (#194)
* View layout is now resolved on initialization (#196)
* Render, update and open process was moved to pipeline interceptors (#196)
* Update mockito monorepo to v4.7.0 (#197)
* Add license scan report and status (#198)
* Pagination source now can be only set once (#201)
* Dynamic layout change is not allowed (#201)
* Handle pagination source and pagination source renderization independently (#201)
* Fix "At least one pagination source must be set.." (#201)
* New shortcut handlers #rendered and #updated to ViewItem (#201)
* Rendering functions allowed back on pagination item render (#201)
* Layout now rendered dynamically when source is updated (#201)
* Fix Bukkit player retrieval on close handler (#202)
* Remove nextTick from updateSlot call (#203)
* Allow users to ignore navigation item factory (#205)
* Slot conversion fix (#208)
* Allow slot clear on update (#209)
* Custom UnknownReferenceException and nullable ref (#210)
* Instantiate entity container properly, fixes `isOnEntityContainer` checks (#211) 

# 2.5.3
* Fix scheduled updates (#175)
* UnsupportedOperationException when use getPlayer in PaginatedViewSlotContext (#183)
* Skip layout render if signature is not checked (#185)
* IllegalStateException when use setSource in PaginatedView (#172)
* Test-only workflow (#154)
* Update code formatter (#173)
* Update plugin com.diffplug.spotless to v6.9.0 (#182)
* Update junit5 monorepo to v5.9.0 (#179)
* Update ASzc/change-string-case-action action to v2 (#168)
* Update bug report issue template (#171)

# 2.5.2
* Nested view open by context ClassCastException (#164)
* Lately fetch update job initiator (#163)
* Context update implementation (#162)
* Change ViewItem click handler type (#161)
* Update gradle to `v7.5` (#155)
* Update gradle/gradle-build-action digest to `cd3cedc` (#157)
* Create generate artifacts workflow (#165)

# 2.5.1
* Click handler doesn't work in onItemRender. Thanks to @Azodox (#142)
* Random NPE when closing inventory by. Thanks to @azodox (#141)
* ConcurrentModificationException in while opening view (#144)
* Dynamic title update implementation (#146)
* Handle close if mark is applied or item is close on click (#148)

# 2.5.1-rc.2
* Fix container size normalization (#133)
* Transfer open context data to regular context (#134)
* Enable bStats metrics on library level (#136)
* Allow multiple ViewFrame instances on non-bundled library (#138)
* Update plugin com.diffplug.spotless to v6.8.0 (#125)
* Update gradle/gradle-build-action digest to 2a7ffc9 (#128)
* Update kotlin to v1.7.10 (#130)

# 2.5.1-rc.1
* Added support for more inventory types ([#58](https://github.com/DevNatan/inventory-framework/issues/58))
* Lazy pagination source ([#72](https://github.com/DevNatan/inventory-framework/issues/72))
* Item References ([#80](https://github.com/DevNatan/inventory-framework/issues/80))
* Internal codebase rewrite with a new pipeline and feature system ([#89](https://github.com/DevNatan/inventory-framework/pull/89))
* ~~Kotlin DSL ([#90](https://github.com/DevNatan/inventory-framework/pull/90))~~
* Fixed the order of interceptors calls
* Prohibit pagination item rendering function use ([#104](https://github.com/DevNatan/inventory-framework/pull/104))
* Fixed a bug where when the paginated view was updated with empty source the items from the previous pagination were not cleared
* Fixed view update job error ([#106](https://github.com/DevNatan/inventory-framework/issues/106))
* Fixed error that causes context to be cleared randomly by the Garbage Collector ([#107](https://github.com/DevNatan/inventory-framework/issues/107))
* Slot context item patch ([#112](https://github.com/DevNatan/inventory-framework/issues/112))
* Now the item from a slot context can be accessed
* More fluent ViewFrame API with chain accessors
* Removed pollution of overridable methods in View that shouldn't be overridden
* `onClickOutside` and `onHotbarInteract` handlers now deprecated
* Now `onItemHold`, `onItemRelease` and `onMoveOut` can be applied per-item
* Experimental per item update schedule, View's `scheduleUpdate` like can be now used per-item
* Now an exception will be thrown if the user does not define any paging data
* Now an exception is thrown when the user changes the state of the view after it has been rendered
* Bukkit platform implementation now have bStats
* User-defined layout pattern method signature changed
* Pagination navigation item methods override now deprecated
* Page switch methods returns a boolean to know if page switch was successful
* Exception handler now catches page switch errors
* Paginated view `offset` and `limit` now deprecated
* Fixed a bug that pagination data of multiple contexts in the same view conflicts with each other
* Asynchronous Pagination (#113)
* Allow user defined layout in context scope

# 2.5
* Inheritable context data (#65)
* User-defined paginated view layout pattern (#70)
* Custom "on click outside" inventory handler (#55)
* Pagination item overrides already set item on re-render (#81)
* Now errors are propagated by default if they occur inside a handler
* View not longer implements `Closeable`
* Replaced `BLAZE_ROW` to `BLAZE_ROD` in readme (thanks to @mattnicee7) (#75)

# 2.4.3
* New `onHotbarInteract(...)` to handle hotbar button interactions;
* Documentation explaining how view options work ([#66][p66]);
* Allow transitive data context while transitioning to another view ([#48][i48]);
* Error handling now works on the initial `onRender` ([#51][i51]);
* Error handling now works on `onOpen` ([#51][i51]);
* It's now allowed to cancel the initial render handler context, causing the inventory to not be displayed to the player ([#60][p60]);
* An exception is thrown when trying to manipulate inventory before opening it ([#63][p63]);
* New PaginatedView(rows) constructor ([#49][i49]);
* Possibility to get the pagination source from ViewContext ([#62][p62]);
* Allow getting current title from context inventory ([#56][p56]);

# 2.4.2
* Previous and next page items aren't displayed if item slot is manually defined ([#44][i44])
* Add Jetbrains annotations as transitive dependency ([#45][i45])
* Error handler is not working in paginated inventory ([#41][i41])

# 2.4.1
* Error Handling prototype.
* No longer possible to use shift-click to move items from the player's inventory to the view's
  inventory that has `cancelOnClick` or `cancelOnShiftClick` enabled.
* Fixed bug where `closeOnOutsideClick` closed the View for all its contexts not just for the player.
* Fixed clicks being cancelled in player inventory if view is marked to cancel on click even 
  player is not clicking in the view inventory.
* Now `closeOnOutsideClick` View option are enabled by default.
* Build project on JDK 8 (thanks to @luiz-otavio [#40][i40])

# 2.4.0
* ~~Detect the movement of an item from the player's inventory to their View using `onMoveIn` 
  feature preview.~~ (postponed to 2.5)
* Some public methods from VirtualView and View become internal, and fast path methods (to ItemStack)
  from VirtualView becomes `@Deprecated` and will be removed soon.
* Allow PaginatedView layout removal by specifying null on `setLayout`.
* Moved `ReflectionUtils` to a different package to prevent conflicts with other 
  libraries/plugins (thanks to @GeorgeV220 ([#31][i31]).
* Slot conversion to near-limited row was fixed ([#32][i32]).
* Introduced a more intuitive way to schedule view updates with`scheduleUpdate`, `ScheduledView` is
  now deprecated and will be removed soon.
* Now an exception is thrown when trying to open a View without the ViewFrame being registered.
* Now is allowed to set items in the `OpenViewContext` they will be static items but only visible for that context.
* Added `setCloseOnClickOutside` option to close View if player clicks outside the inventory screen.
* `CloseViewContext` now delegates the original context (thanks to @zAlyson).
* An exception will be thrown when an inventory tries to be modified in the `onOpen` View handler.

# 2.3.2
* Dynamic title update (`ViewContext#updateTitle`) support on 1.17 and 1.18 ([#25][p25]).
* Expose `VirtualView.toSlot` to convert slot numbers.
* Some contexts came with null click origin ([#27][p27]).
* Now an exception is thrown when a View is registered multiple times.
* `onItemRelease` now works with PaginatedView ([#26][p26]).
* Some methods of the ViewFrame class have been marked ***@Deprecated*** as they will be removed in the near future and should not be used.

# 2.3.1
* Assign deprecated annotation in View#getRows for dynamic views([#19][p19])
* View auto update and Spigot 1.16.5 API support ([#18][p18])

[p18]: https://github.com/DevNatan/inventory-framework/pull/18
[p19]: https://github.com/DevNatan/inventory-framework/pull/19
[p25]: https://github.com/DevNatan/inventory-framework/pull/25
[p26]: https://github.com/DevNatan/inventory-framework/pull/26
[p27]: https://github.com/DevNatan/inventory-framework/pull/27
[p56]: https://github.com/DevNatan/inventory-framework/pull/56
[p60]: https://github.com/DevNatan/inventory-framework/pull/60
[p62]: https://github.com/DevNatan/inventory-framework/pull/62
[p63]: https://github.com/DevNatan/inventory-framework/pull/63
[p66]: https://github.com/DevNatan/inventory-framework/pull/66
[i31]: https://github.com/DevNatan/inventory-framework/issues/31
[i32]: https://github.com/DevNatan/inventory-framework/issues/32
[i40]: https://github.com/DevNatan/inventory-framework/issues/40
[i41]: https://github.com/DevNatan/inventory-framework/issues/41
[i44]: https://github.com/DevNatan/inventory-framework/issues/44
[i45]: https://github.com/DevNatan/inventory-framework/issues/45
[i48]: https://github.com/DevNatan/inventory-framework/issues/48
[i49]: https://github.com/DevNatan/inventory-framework/issues/49
[i51]: https://github.com/DevNatan/inventory-framework/issues/51
