# Phase 7: Recipe & Data Generation Migration Plan

**Target:** Forge 1.20.1 (47.x) -> NeoForge 1.21.1 (21.1.x)
**Prerequisites:** Phase 6 complete (entity data serialization migrated)
**Estimated effort:** LOW-MEDIUM
**Files affected:** 9

---

## Phase Overview

Phase 7 migrates the data generation and recipe systems. The key API changes are:

1. **RecipeProvider:** `buildRecipes(Consumer<FinishedRecipe>)` -> `buildRecipes(RecipeOutput)`
2. **HolderLookup.Provider:** Added as constructor parameter to `RecipeProvider` and `LootTableProvider`
3. **CustomRecipe:** No longer takes `ResourceLocation` in constructor
4. **SimpleCraftingRecipeSerializer:** Factory lambda loses `ResourceLocation` parameter
5. **Import relocations:** `net.minecraftforge.*` -> `net.neoforged.neoforge.*` across all data gen providers

### IMPORTANT: Items That Do NOT Apply to 1.21.1

> **C-5 RESOLVED:** The following items were incorrectly included in some earlier analyses.
> They belong to MC 1.21.2+ and **must NOT be implemented** in this migration:
>
> - **Runner inner class pattern** in RecipeProvider — does NOT exist in NeoForge 1.21.1
> - **GatherDataEvent split** into client/server variants — does NOT exist in NeoForge 1.21.1
>
> In NeoForge 1.21.1, `GatherDataEvent` remains unified and `RecipeProvider` does NOT use a Runner inner class.

---

## Step-by-Step Migration

### Step 1: ModRecipeProvider.java

**File:** `src/main/java/dev/murad/shipping/data/ModRecipeProvider.java`

**Current code:**
```java
import net.minecraft.data.recipes.*;
import net.minecraftforge.common.Tags;
import java.util.function.Consumer;

public class ModRecipeProvider extends RecipeProvider {
    public ModRecipeProvider(PackOutput packOutput) {
        super(packOutput);
    }

    @Override
    protected void buildRecipes(Consumer<FinishedRecipe> consumer) {
        ShapedRecipeBuilder.shaped(...)
            .save(consumer);
        // ... 30+ recipe builders
    }
}
```

**Target code:**
```java
import net.minecraft.data.recipes.*;
import net.neoforged.neoforge.common.Tags;   // L-4: import update
import net.minecraft.core.HolderLookup;
import java.util.concurrent.CompletableFuture;

public class ModRecipeProvider extends RecipeProvider {
    public ModRecipeProvider(PackOutput packOutput, CompletableFuture<HolderLookup.Provider> registries) {
        super(packOutput, registries);
    }

    @Override
    protected void buildRecipes(RecipeOutput output) {
        ShapedRecipeBuilder.shaped(...)
            .save(output);
        // ... all 30+ .save(consumer) calls become .save(output)
        // NO other changes to recipe builder calls
    }
}
```

**Changes:**
1. Add `CompletableFuture<HolderLookup.Provider>` constructor parameter, pass to `super()`
2. Change method signature: `buildRecipes(Consumer<FinishedRecipe> consumer)` -> `buildRecipes(RecipeOutput output)`
3. Remove `import java.util.function.Consumer;`
4. Update `net.minecraftforge.common.Tags` -> `net.neoforged.neoforge.common.Tags` (L-4)
5. Rename all `consumer` references to `output` (or just change parameter name)
6. **Do NOT add a Runner inner class** (1.21.2+ only)

### Step 2: AbstractRouteCopyRecipe.java (Gap M-7)

**File:** `src/main/java/dev/murad/shipping/recipe/AbstractRouteCopyRecipe.java`

**Current code:**
```java
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;

public abstract class AbstractRouteCopyRecipe extends CustomRecipe {
    private final Item item;
    public AbstractRouteCopyRecipe(ResourceLocation resourceLocation, CraftingBookCategory cat, Item matchingItem) {
        super(resourceLocation, cat);
        this.item = matchingItem;
    }

    @Override
    public ItemStack assemble(@Nonnull CraftingContainer inventory, @NotNull RegistryAccess pRegistryAccess) {
        // ...
    }
}
```

**Target code:**
```java
import net.minecraft.core.HolderLookup;
// Remove: import net.minecraft.resources.ResourceLocation;

public abstract class AbstractRouteCopyRecipe extends CustomRecipe {
    private final Item item;
    public AbstractRouteCopyRecipe(CraftingBookCategory cat, Item matchingItem) {
        super(cat);
        this.item = matchingItem;
    }

    @Override
    public ItemStack assemble(@Nonnull CraftingContainer inventory, HolderLookup.@NotNull Provider registries) {
        // ... body unchanged
    }
}
```

**Changes:**
1. Remove `ResourceLocation` from constructor parameter and `super()` call
2. `assemble()` signature: `RegistryAccess` -> `HolderLookup.Provider` (Gap M-7)
3. Remove `import net.minecraft.core.RegistryAccess;`
4. Add `import net.minecraft.core.HolderLookup;`
5. Remove `import net.minecraft.resources.ResourceLocation;` (no longer needed)

### Step 3: ModRecipeSerializers.java

**File:** `src/main/java/dev/murad/shipping/setup/ModRecipeSerializers.java`

**Current code:**
```java
import net.minecraftforge.registries.RegistryObject;

public class ModRecipeSerializers {
    public static final RegistryObject<SimpleCraftingRecipeSerializer<AbstractRouteCopyRecipe>> TUG_ROUTE_COPY =
        Registration.RECIPE_SERIALIZERS.register(
            "tug_route_copy", () -> new SimpleCraftingRecipeSerializer<>(
                (loc, cat) -> new AbstractRouteCopyRecipe(loc, cat, ModItems.TUG_ROUTE.get()) {
                    // ...
                }));

    public static final RegistryObject<SimpleCraftingRecipeSerializer<AbstractRouteCopyRecipe>> LOCO_ROUTE_COPY =
        Registration.RECIPE_SERIALIZERS.register(
            "loco_route_copy", () -> new SimpleCraftingRecipeSerializer<>(
                (loc, cat) -> new AbstractRouteCopyRecipe(loc, cat, ModItems.LOCO_ROUTE.get()) {
                    // ...
                }));
}
```

**Target code:**
```java
import net.neoforged.neoforge.registries.DeferredHolder;  // or DeferredRegister pattern

public class ModRecipeSerializers {
    public static final DeferredHolder<RecipeSerializer<?>, SimpleCraftingRecipeSerializer<AbstractRouteCopyRecipe>> TUG_ROUTE_COPY =
        Registration.RECIPE_SERIALIZERS.register(
            "tug_route_copy", () -> new SimpleCraftingRecipeSerializer<>(
                (cat) -> new AbstractRouteCopyRecipe(cat, ModItems.TUG_ROUTE.get()) {
                    // ...
                }));

    public static final DeferredHolder<RecipeSerializer<?>, SimpleCraftingRecipeSerializer<AbstractRouteCopyRecipe>> LOCO_ROUTE_COPY =
        Registration.RECIPE_SERIALIZERS.register(
            "loco_route_copy", () -> new SimpleCraftingRecipeSerializer<>(
                (cat) -> new AbstractRouteCopyRecipe(cat, ModItems.LOCO_ROUTE.get()) {
                    // ...
                }));
}
```

**Changes:**
1. Lambda `(loc, cat) -> new AbstractRouteCopyRecipe(loc, cat, item)` becomes `(cat) -> new AbstractRouteCopyRecipe(cat, item)`
2. `RegistryObject` -> `DeferredHolder` (depends on Phase 2 registration migration)
3. Remove `loc` parameter — `ResourceLocation` is now managed by the registry

### Step 4: ModLootTableProvider.java

**File:** `src/main/java/dev/murad/shipping/data/ModLootTableProvider.java`

**Current code:**
```java
import net.minecraftforge.registries.RegistryObject;

public class ModLootTableProvider extends LootTableProvider {
    public ModLootTableProvider(PackOutput output) {
        super(output, Set.of(), ImmutableList.of(
            new SubProviderEntry(ModBlockLootTables::new, LootContextParamSets.BLOCK)
        ));
    }

    public static class ModBlockLootTables extends BlockLootSubProvider {
        // ...
        @Override
        public @NotNull Iterable<Block> getKnownBlocks() {
            return Registration.BLOCKS.getEntries().stream()
                .map(RegistryObject::get)
                .collect(Collectors.toList());
        }
    }
}
```

**Target code:**
```java
import net.minecraft.core.HolderLookup;
import java.util.concurrent.CompletableFuture;
// RegistryObject -> DeferredHolder (if Phase 2 complete)

public class ModLootTableProvider extends LootTableProvider {
    public ModLootTableProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, Set.of(), ImmutableList.of(
            new SubProviderEntry(ModBlockLootTables::new, LootContextParamSets.BLOCK)
        ), registries);
    }

    public static class ModBlockLootTables extends BlockLootSubProvider {
        // ...
        @Override
        public @NotNull Iterable<Block> getKnownBlocks() {
            return Registration.BLOCKS.getEntries().stream()
                .map(DeferredHolder::get)    // RegistryObject -> DeferredHolder
                .collect(Collectors.toList());
        }
    }
}
```

**Changes:**
1. Add `CompletableFuture<HolderLookup.Provider> registries` constructor parameter
2. Pass `registries` as 4th argument to `super()` constructor
3. `RegistryObject::get` -> `DeferredHolder::get` (depends on Phase 2)

### Step 5: DataGenerators.java

**File:** `src/main/java/dev/murad/shipping/data/DataGenerators.java`

**Current code:**
```java
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.data.event.GatherDataEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = ShippingMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class DataGenerators {
    @SubscribeEvent
    public static void gatherData(GatherDataEvent gatherDataEvent) {
        var gen = gatherDataEvent.getGenerator();
        var existingFileHelper = gatherDataEvent.getExistingFileHelper();
        var pack = gen.getPackOutput();
        var lookupProvider = gatherDataEvent.getLookupProvider();

        gen.addProvider(true, new ModBlockStateProvider(pack, existingFileHelper));
        gen.addProvider(true, new ModItemModelProvider(pack, existingFileHelper));

        var blockTags = new ModBlockTagsProvider(pack, lookupProvider, existingFileHelper);
        gen.addProvider(true, blockTags);
        gen.addProvider(true, new ModItemTagsProvider(pack, lookupProvider, blockTags.contentsGetter(), existingFileHelper));
        gen.addProvider(true, new ModLootTableProvider(pack));
        gen.addProvider(true, new ModRecipeProvider(pack));
    }
}
```

**Target code:**
```java
import net.neoforged.neoforge.common.data.ExistingFileHelper;       // L-4
import net.neoforged.neoforge.data.event.GatherDataEvent;            // L-4
import net.neoforged.bus.api.SubscribeEvent;                          // L-4
import net.neoforged.fml.common.Mod;                                  // L-4

@Mod.EventBusSubscriber(modid = ShippingMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class DataGenerators {
    @SubscribeEvent
    public static void gatherData(GatherDataEvent gatherDataEvent) {
        var gen = gatherDataEvent.getGenerator();
        var existingFileHelper = gatherDataEvent.getExistingFileHelper();
        var pack = gen.getPackOutput();
        var lookupProvider = gatherDataEvent.getLookupProvider();

        gen.addProvider(true, new ModBlockStateProvider(pack, existingFileHelper));
        gen.addProvider(true, new ModItemModelProvider(pack, existingFileHelper));

        var blockTags = new ModBlockTagsProvider(pack, lookupProvider, existingFileHelper);
        gen.addProvider(true, blockTags);
        gen.addProvider(true, new ModItemTagsProvider(pack, lookupProvider, blockTags.contentsGetter(), existingFileHelper));
        gen.addProvider(true, new ModLootTableProvider(pack, lookupProvider));   // pass lookupProvider
        gen.addProvider(true, new ModRecipeProvider(pack, lookupProvider));      // pass lookupProvider
    }
}
```

**Changes:**
1. All `net.minecraftforge.*` imports -> `net.neoforged.*` equivalents (L-4)
2. Pass `lookupProvider` to `ModLootTableProvider` and `ModRecipeProvider` constructors
3. **GatherDataEvent stays UNIFIED** — do NOT split into client/server (1.21.2+ only)
4. Method body structure remains the same

### Step 6: ModBlockTagsProvider.java (Gap L-4)

**File:** `src/main/java/dev/murad/shipping/data/ModBlockTagsProvider.java`

**Current code:**
```java
import net.minecraftforge.common.data.BlockTagsProvider;
import net.minecraftforge.common.data.ExistingFileHelper;
```

**Target code:**
```java
import net.neoforged.neoforge.common.data.BlockTagsProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
```

**Changes:** Import path update only (L-4). Constructor and `addTags()` body unchanged.

### Step 7: ModItemTagsProvider.java (Gap L-4)

**File:** `src/main/java/dev/murad/shipping/data/ModItemTagsProvider.java`

**Current code:**
```java
import net.minecraftforge.common.data.BlockTagsProvider;
import net.minecraftforge.common.data.ExistingFileHelper;
```

**Target code:**
```java
import net.neoforged.neoforge.common.data.BlockTagsProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
```

**Changes:** Import path update only (L-4). Constructor and `addTags()` body unchanged.

### Step 8: ModBlockStateProvider.java (Gap L-4)

**File:** `src/main/java/dev/murad/shipping/data/client/ModBlockStateProvider.java`

**Current code:**
```java
import net.minecraftforge.client.model.generators.BlockModelBuilder;
import net.minecraftforge.client.model.generators.BlockStateProvider;
import net.minecraftforge.client.model.generators.ConfiguredModel;
import net.minecraftforge.client.model.generators.ModelFile;
import net.minecraftforge.common.data.ExistingFileHelper;
```

**Target code:**
```java
import net.neoforged.neoforge.client.model.generators.BlockModelBuilder;
import net.neoforged.neoforge.client.model.generators.BlockStateProvider;
import net.neoforged.neoforge.client.model.generators.ConfiguredModel;
import net.neoforged.neoforge.client.model.generators.ModelFile;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
```

**Additional changes:**
- `new ResourceLocation(namespace, path)` -> `ResourceLocation.fromNamespaceAndPath(namespace, path)` at line 32 (and any other occurrences) — this may be deferred to Phase 9B mechanical cleanup

### Step 9: ModItemModelProvider.java (Gap L-4)

**File:** `src/main/java/dev/murad/shipping/data/client/ModItemModelProvider.java`

**Current code:**
```java
import net.minecraftforge.client.model.generators.ItemModelBuilder;
import net.minecraftforge.client.model.generators.ItemModelProvider;
import net.minecraftforge.client.model.generators.ModelFile;
import net.minecraftforge.common.data.ExistingFileHelper;
```

**Target code:**
```java
import net.neoforged.neoforge.client.model.generators.ItemModelBuilder;
import net.neoforged.neoforge.client.model.generators.ItemModelProvider;
import net.neoforged.neoforge.client.model.generators.ModelFile;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
```

**Additional changes:**
- `new ResourceLocation(...)` at lines 50, 55, 74 -> `ResourceLocation.fromNamespaceAndPath(...)` — may be deferred to Phase 9B

---

## Gap Tracking

| Gap ID | Description | File(s) | Status |
|--------|-------------|---------|--------|
| M-7 | `assemble()` RegistryAccess -> HolderLookup.Provider | AbstractRouteCopyRecipe.java | Step 2 |
| L-4 | Data gen provider import updates (minecraftforge -> neoforged) | All 7 data/ files | Steps 5-9 |
| C-5 | Runner class + GatherDataEvent split do NOT apply | N/A | Explicitly excluded |

---

## Execution Order

1. **Step 2 first** — AbstractRouteCopyRecipe (constructor change propagates)
2. **Step 3** — ModRecipeSerializers (depends on Step 2 constructor change)
3. **Step 1** — ModRecipeProvider (independent)
4. **Step 4** — ModLootTableProvider (independent)
5. **Step 5** — DataGenerators (depends on Steps 1 + 4 for new constructor params)
6. **Steps 6-9** — Tag/model providers (independent, import-only changes)

Steps 1, 4, 6-9 can be done in parallel. Steps 2->3 must be sequential. Step 5 must come after Steps 1 and 4.

---

## Verification Steps

1. **Compile check:** `./gradlew build` — must succeed with no errors in `data/` or `recipe/` packages
2. **Data generation:** `./gradlew runData` — must succeed and produce output in `src/generated/resources/`
3. **Verify generated output:** Diff `src/generated/resources/` before and after migration — recipe JSON, loot table JSON, block state JSON, item model JSON, and tag JSON should be **identical** to pre-migration output
4. **Unit tests:** `./gradlew test` — existing tests must still pass

### Smoke test checklist:
- [ ] `./gradlew runData` completes without errors
- [ ] All recipe JSON files are generated in `data/littlelogistics/recipes/`
- [ ] All loot table JSON files are generated in `data/littlelogistics/loot_tables/`
- [ ] All tag JSON files are generated in `data/littlelogistics/tags/`
- [ ] All block state/model JSON files are generated in `assets/littlelogistics/`
- [ ] `./gradlew build` succeeds
