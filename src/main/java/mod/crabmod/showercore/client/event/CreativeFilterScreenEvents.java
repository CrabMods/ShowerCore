package mod.crabmod.showercore.client.event;

import com.crabmod.hotbath.item.ItemGroup;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import mod.crabmod.showercore.ShowerCore;
import mod.crabmod.showercore.client.gui.widget.CreativeFilterIconButton;
import mod.crabmod.showercore.client.gui.widget.CreativeFilterTagButton;
import mod.crabmod.showercore.common.ShowerCoreItemTags;
import mod.crabmod.showercore.registers.BlocksRegister;
import mod.crabmod.showercore.registers.ItemRegister;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class CreativeFilterScreenEvents {
  private static final int VISIBLE_FILTERS = 4;
  private static final String HOT_BATH_NAMESPACE = "hotbath";
  private static int startIndex;

  private List<TagFilter> filters;
  private List<CreativeFilterTagButton> buttons;
  private List<ItemStack> baseTabItems;
  private TagFilter hotBathFluidsFilter;
  private TagFilter accessoriesFilter;
  private Button btnScrollUp;
  private Button btnScrollDown;
  private Button btnEnableAll;
  private Button btnDisableAll;
  private CreativeModeTab lastTab = CreativeModeTabs.getDefaultTab();
  private int guiLeft;
  private int guiTop;

  private static CreativeModeTab getSelectedTab() {
    try {
      java.lang.reflect.Field field =
          CreativeModeInventoryScreen.class.getDeclaredField("selectedTab");
      field.setAccessible(true);
      return (CreativeModeTab) field.get(null);
    } catch (ReflectiveOperationException | ClassCastException e) {
      return CreativeModeTabs.getDefaultTab();
    }
  }

  @SubscribeEvent
  public void onPlayerLogout(ClientPlayerNetworkEvent.LoggingOut event) {
    this.filters = null;
    this.baseTabItems = null;
    startIndex = 0;
  }

  @SubscribeEvent
  public void onScreenInit(ScreenEvent.Init.Post event) {
    if (event.getScreen() instanceof CreativeModeInventoryScreen creativeScreen) {
      if (this.filters == null) {
        this.compileItems();
      }

      this.guiLeft = creativeScreen.getGuiLeft();
      this.guiTop = creativeScreen.getGuiTop();
      this.buttons = this.createTagButtons();
      this.buttons.forEach(event::addListener);

      event.addListener(
          this.btnScrollUp =
              new CreativeFilterIconButton(
                  this.guiLeft - 22,
                  this.guiTop - 12,
                  CreativeFilterIconButton.Icon.SCROLL_UP,
                  Component.translatable("gui.showercore.filter.scroll_up"),
                  button -> {
                    if (startIndex > 0) {
                      startIndex--;
                    }
                    this.updateTagButtons();
                  }));

      event.addListener(
          this.btnScrollDown =
              new CreativeFilterIconButton(
                  this.guiLeft - 22,
                  this.guiTop + 127,
                  CreativeFilterIconButton.Icon.SCROLL_DOWN,
                  Component.translatable("gui.showercore.filter.scroll_down"),
                  button -> {
                    if (startIndex <= this.filters.size() - VISIBLE_FILTERS - 1) {
                      startIndex++;
                    }
                    this.updateTagButtons();
                  }));

      event.addListener(
          this.btnEnableAll =
              new CreativeFilterIconButton(
                  this.guiLeft - 50,
                  this.guiTop + 10,
                  CreativeFilterIconButton.Icon.ENABLE_ALL,
                  Component.translatable("gui.showercore.filter.enable_all"),
                  button -> {
                    this.filters.forEach(filter -> filter.setEnabled(true));
                    this.buttons.forEach(CreativeFilterTagButton::updateState);
                    this.updateCurrentCreativeScreen();
                  }));

      event.addListener(
          this.btnDisableAll =
              new CreativeFilterIconButton(
                  this.guiLeft - 50,
                  this.guiTop + 32,
                  CreativeFilterIconButton.Icon.DISABLE_ALL,
                  Component.translatable("gui.showercore.filter.disable_all"),
                  button -> {
                    this.filters.forEach(filter -> filter.setEnabled(false));
                    this.buttons.forEach(CreativeFilterTagButton::updateState);
                    this.updateCurrentCreativeScreen();
                  }));

      CreativeModeTab selected = getSelectedTab();
      this.onSwitchCreativeTab(selected, creativeScreen);
      this.lastTab = selected;
    }
  }

  @SubscribeEvent
  public void onScreenDrawPost(ScreenEvent.Render.Post event) {
    if (event.getScreen() instanceof CreativeModeInventoryScreen creativeScreen) {
      this.guiLeft = creativeScreen.getGuiLeft();
      this.guiTop = creativeScreen.getGuiTop();

      CreativeModeTab tab = getSelectedTab();
      if (this.lastTab != tab) {
        this.onSwitchCreativeTab(tab, creativeScreen);
        this.lastTab = tab;
      }
    }
  }

  private void onSwitchCreativeTab(CreativeModeTab tab, CreativeModeInventoryScreen screen) {
    boolean targetTab = this.isTargetCreativeTab(tab);
    this.setControlsVisible(targetTab);
    if (targetTab) {
      this.captureBaseTabItems(screen);
      this.updateTagButtons();
      this.updateItems(screen);
    }
  }

  private boolean isTargetCreativeTab(CreativeModeTab tab) {
    try {
      return tab == ItemGroup.HOT_BATH_TAB.get();
    } catch (Throwable e) {
      return false;
    }
  }

  private void setControlsVisible(boolean visible) {
    if (this.btnScrollUp != null) {
      this.btnScrollUp.visible = visible;
      this.btnScrollDown.visible = visible;
      this.btnEnableAll.visible = visible;
      this.btnDisableAll.visible = visible;
    }
    if (this.buttons != null) {
      this.buttons.forEach(button -> button.visible = false);
    }
  }

  private List<CreativeFilterTagButton> createTagButtons() {
    List<CreativeFilterTagButton> tagButtons = new ArrayList<>();
    for (TagFilter filter : this.filters) {
      CreativeFilterTagButton tagButton =
          new CreativeFilterTagButton(
              this.guiLeft - 28,
              this.guiTop,
              filter,
              button -> this.updateCurrentCreativeScreen());
      tagButton.visible = false;
      tagButtons.add(tagButton);
    }
    return tagButtons;
  }

  private void updateTagButtons() {
    if (this.buttons == null) {
      return;
    }
    this.buttons.forEach(button -> button.visible = false);
    for (int i = startIndex; i < startIndex + VISIBLE_FILTERS && i < this.buttons.size(); i++) {
      CreativeFilterTagButton button = this.buttons.get(i);
      button.setY(this.guiTop + 29 * (i - startIndex) + 10);
      button.visible = true;
    }
    this.btnScrollUp.active = startIndex > 0;
    this.btnScrollDown.active = startIndex <= this.filters.size() - VISIBLE_FILTERS - 1;
  }

  private void updateCurrentCreativeScreen() {
    Screen screen = Minecraft.getInstance().screen;
    if (screen instanceof CreativeModeInventoryScreen creativeScreen) {
      this.updateItems(creativeScreen);
    }
  }

  private void captureBaseTabItems(CreativeModeInventoryScreen screen) {
    if (this.baseTabItems != null) {
      return;
    }
    this.captureHostCategoryStacks(screen);
    Set<Item> filteredItems = this.getFilteredItems();
    this.baseTabItems = new ArrayList<>();
    for (ItemStack stack : screen.getMenu().items) {
      if (!stack.isEmpty() && !filteredItems.contains(stack.getItem())) {
        this.baseTabItems.add(stack.copy());
      }
    }
  }

  private void updateItems(CreativeModeInventoryScreen screen) {
    CreativeModeInventoryScreen.ItemPickerMenu menu = screen.getMenu();
    List<ItemStack> categorizedStacks = new ArrayList<>();
    for (TagFilter filter : this.filters) {
      if (filter.isEnabled()) {
        for (ItemStack stack : filter.getStacks()) {
          categorizedStacks.add(stack.copy());
        }
      }
    }

    NonNullList<ItemStack> newItems = NonNullList.create();
    if (this.baseTabItems != null) {
      for (ItemStack stack : this.baseTabItems) {
        newItems.add(stack.copy());
      }
    }
    for (ItemStack stack : categorizedStacks) {
      newItems.add(stack.copy());
    }

    menu.items.clear();
    menu.items.addAll(newItems);
    menu.items.sort(Comparator.comparingInt(stack -> Item.getId(stack.getItem())));
    menu.scrollTo(0);
  }

  private Set<Item> getFilteredItems() {
    LinkedHashSet<Item> items = new LinkedHashSet<>();
    for (TagFilter filter : this.filters) {
      for (ItemStack stack : filter.getStacks()) {
        items.add(stack.getItem());
      }
    }
    return items;
  }

  private void compileItems() {
    TagFilter hotBathFluids =
        new TagFilter(
            Component.translatable("gui.showercore.filter.hot_bath_fluids"),
            new ItemStack(com.crabmod.hotbath.registers.ItemRegister.HOT_WATER_BUCKET.get()));
    TagFilter accessories =
        new TagFilter(
            ShowerCoreItemTags.ACCESSORIES,
            Component.translatable("gui.showercore.filter.accessories"),
            new ItemStack(ItemRegister.RUBBER_DUCK.get()));
    TagFilter[] compiledFilters =
        new TagFilter[] {
          hotBathFluids,
          new TagFilter(
              ShowerCoreItemTags.BATH_CORES,
              Component.translatable("gui.showercore.filter.bath_cores"),
              new ItemStack(BlocksRegister.HOT_WATER_CORE.get())),
          new TagFilter(
              ShowerCoreItemTags.SHOWER_HEADS,
              Component.translatable("gui.showercore.filter.shower_heads"),
              new ItemStack(BlocksRegister.RAIN_SHOWER_HEAD_WHITE.get())),
          new TagFilter(
              ShowerCoreItemTags.BATHTUBS,
              Component.translatable("gui.showercore.filter.bathtubs"),
              new ItemStack(BlocksRegister.BATHTUB_WHITE.get())),
          new TagFilter(
              ShowerCoreItemTags.CLAWFOOT_BATHTUBS,
              Component.translatable("gui.showercore.filter.clawfoot_bathtubs"),
              new ItemStack(BlocksRegister.BATHTUB_CLAWFOOT_WHITE.get())),
          accessories
        };

    ItemGroup.HOT_BATH_TAB.get().getDisplayItems().stream()
        .map(ItemStack::getItem)
        .filter(CreativeFilterScreenEvents::isShowerCoreItem)
        .forEach(
            item ->
                BuiltInRegistries.ITEM
                    .getHolder(BuiltInRegistries.ITEM.getId(item))
                    .ifPresent(
                        holder ->
                            holder
                                .tags()
                                .forEach(
                                    tagKey -> {
                                      for (TagFilter filter : compiledFilters) {
                                        if (filter.getTag() != null
                                            && Objects.equals(tagKey, filter.getTag())) {
                                          filter.add(item);
                                        }
                                      }
                                    })));

    for (TagFilter filter : compiledFilters) {
      if (filter.getTag() != null && filter.getStacks().isEmpty()) {
        this.addFallbackItems(filter);
      }
    }

    this.hotBathFluidsFilter = hotBathFluids;
    this.accessoriesFilter = accessories;
    this.filters = new ArrayList<>(Arrays.asList(compiledFilters));
  }

  private void captureHostCategoryStacks(CreativeModeInventoryScreen screen) {
    for (ItemStack stack : screen.getMenu().items) {
      if (this.isHotBathFluidContainer(stack)) {
        this.hotBathFluidsFilter.add(stack);
      } else if (this.isHotBathAccessory(stack)) {
        this.accessoriesFilter.add(stack);
      }
    }
  }

  private void addFallbackItems(TagFilter filter) {
    ItemGroup.HOT_BATH_TAB.get().getDisplayItems().stream()
        .map(ItemStack::getItem)
        .filter(CreativeFilterScreenEvents::isShowerCoreItem)
        .forEach(
            item -> {
              ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
              if (id != null && fallbackMatches(filter.getTag(), id.getPath())) {
                filter.add(item);
              }
            });
  }

  private static boolean isShowerCoreItem(Item item) {
    ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
    return id != null && ShowerCore.MODID.equals(id.getNamespace());
  }

  private boolean isHotBathFluidContainer(ItemStack stack) {
    ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
    if (id == null || !HOT_BATH_NAMESPACE.equals(id.getNamespace())) {
      return false;
    }
    String path = id.getPath();
    return path.endsWith("_bucket") || path.endsWith("_bottle");
  }

  private boolean isHotBathAccessory(ItemStack stack) {
    ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
    return id != null
        && HOT_BATH_NAMESPACE.equals(id.getNamespace())
        && "bath_herb".equals(id.getPath());
  }

  private static boolean fallbackMatches(TagKey<Item> tag, String path) {
    if (tag == ShowerCoreItemTags.BATH_CORES) {
      return path.endsWith("_core");
    }
    if (tag == ShowerCoreItemTags.SHOWER_HEADS) {
      return path.contains("shower_head");
    }
    if (tag == ShowerCoreItemTags.CLAWFOOT_BATHTUBS) {
      return path.startsWith("bathtub_clawfoot_");
    }
    if (tag == ShowerCoreItemTags.BATHTUBS) {
      return path.startsWith("bathtub_") && !path.startsWith("bathtub_clawfoot_");
    }
    if (tag == ShowerCoreItemTags.ACCESSORIES) {
      return path.startsWith("rubber_duck");
    }
    return false;
  }

  public static class TagFilter {
    private final List<ItemStack> stacks = new ArrayList<>();
    private final TagKey<Item> tag;
    private final Component name;
    private final ItemStack icon;
    private boolean enabled = true;

    private TagFilter(Component name, ItemStack icon) {
      this(null, name, icon);
    }

    private TagFilter(TagKey<Item> tag, Component name, ItemStack icon) {
      this.tag = tag;
      this.name = name;
      this.icon = icon;
    }

    public TagKey<Item> getTag() {
      return this.tag;
    }

    public ItemStack getIcon() {
      return this.icon;
    }

    public Component getName() {
      return this.name;
    }

    public void setEnabled(boolean enabled) {
      this.enabled = enabled;
    }

    public boolean isEnabled() {
      return this.enabled;
    }

    public void add(Item item) {
      this.add(new ItemStack(item));
    }

    public void add(Block block) {
      this.add(Item.byBlock(block));
    }

    public void add(ItemStack stack) {
      if (!stack.isEmpty()) {
        this.stacks.add(stack.copy());
      }
    }

    public List<ItemStack> getStacks() {
      return this.stacks;
    }
  }
}
