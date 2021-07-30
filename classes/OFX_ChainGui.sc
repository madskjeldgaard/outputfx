/*

The gui is controlled has a timer (SkipJack) attached. It checks whether the gui objects are out of sync with the proxy. If it is, it will copy the proxy's values to the gui objects' data space.

*/

OFX_ChainGui{
  var <window, <chain, <guiObjects, <guiData, <skipjack, <isMain, <slotNames, <slotsInUse, <preset;

  var title, sourceKeys, slotSections,transportButtons, presetButtons;

  *new{|proxychain| 
    ^super.new.init(proxychain)
  }

  init{|proxychain|
    chain = if(
      proxychain.isKindOf(OFX_OutputFX), 
      { 
        isMain = true;
        "Is main output".postln;
        proxychain.proxyChain 
      }, { 
        isMain = false;
        proxychain 
      });


    preset = OFX_ChainPreset.new(chain);

    this.makeGui();

    window.front();

    ^this
  }

  winName{
    ^chain.key.asString;
  }

  makeGui{

    var layout;

    window = window ?? {Window.new(name: this.winName)};
    window.view.removeAll;

    guiObjects = guiObjects ?? { IdentityDictionary.new };
    guiData = guiData ?? { IdentityDictionary.new };

    slotNames = chain.slotNames.copy();
    sourceKeys = OFX_Chain.allSources.keys;

    title = StaticText.new()
    .string_(chain.key)
    .font_(Font.default.bold_(true));

    slotSections = this.makeAllSlotSections(6);

    // @TODO
    // transportButtons = if(isMain, { nil }, {
    //   this.makeTransportSection() 
    // });

    // @TODO
    presetButtons = this.makePresetSection();

    this.makeGuiData();
    skipjack =skipjack ?? {this.makeSkipjack()};

    this.checkAllActiveSlots();
    layout = HLayout( 
      // [VLayout(
      //   [title, stretch: 1, align: \topLeft], 
      //   [transportButtons, stretch: 2, align: \topLeft], 
      //   [presetButtons, stretch: 3, align: \topLeft],
      //   [nil],
      //   [nil]
      // ), \s: 1], 
      [VLayout(
        [title, stretch: 1, align: \topLeft], 
        [transportButtons, stretch: 2, align: \topLeft], 
        [presetButtons, stretch: 3, align: \topLeft],

        StaticText.new().string_("slots: "), 
        slotSections
      ), \s: 3]
    );

    window.layout_(layout);

  }

  makeAllSlotSections{|clumpInto=3|
    ^VLayout(
      *slotNames.asArray.clump(clumpInto).collect{|clumpedSlots|
        HLayout(*clumpedSlots.collect{|sourceKey| this.makeSlotSection(sourceKey) })
      }
    )
  }

  makeSlotSection{|sourceKey|
    var wetness, items;
    var toggleSlotButton = Button.new() 
    .states_([
      [sourceKey, Color.black, Color.red],
      [sourceKey, Color.black, Color.green],
    ])
    .value_(
      if(chain.isSlotActive(sourceKey), { 1 }, { 0 })
    )
    .action_({|obj| 
      if(obj.value == 1, {
        // Add source
        chain.add(sourceKey)
      }, {
        // Remove source
        chain.remove(sourceKey)
      })
    });

    var currentWetness = OFX_Chain.atSrcDict(sourceKey).level;
    // var currentLevel = if(guiObjects.notNil && guiObjects.isEmpty.not, { 
    //   if(guiObjects[sourceKey][\wetnessSlider].notNil, {
    //     guiObjects[sourceKey][\wetnessSlider].value
    //     }, {
    //       OFX_Chain.atSrcDict(sourceKey).level
    //     })
    // });

    // var wetness = Slider.new()
    // .orientation_(\horizontal)
    // .action_({|obj| 
    //   chain.setWet(sourceKey, obj.value)
    //   // OFX_Chain.atSrcDict(sourceKey).postln.level = obj.value
    // });

    var parameterSliders = VLayout(
      *this.slidersForSlot(sourceKey)
    );

    // @TODO: make blank space flexible 
    var blankSpace = 12;

    var randomizeSectionButton = Button.new() 
    .states_([
      ["rand"],
    ])
    .action_({|obj| 
        chain.randomizeSlot(sourceKey)
    });


    wetness = Slider.new()
        .orientation_(\horizontal)
        .value_(currentWetness)
        .action_({|obj| 
          chain.setWet(sourceKey, obj.value)
          // OFX_Chain.atSrcDict(sourceKey).postln.level = obj.value
        });

    // Set slider after the fact
    // wetness.valueAction_(currentWetness);

    guiObjects[sourceKey][\wetnessSlider] = wetness;
    guiObjects[sourceKey][\toggleSlotButton] = toggleSlotButton;

    items = [
      HLayout([toggleSlotButton, stretch: 2], [randomizeSectionButton, stretch: 1]), 
      wetness,
      parameterSliders, 
      // blankSpace
    ];

    items = items.collect{ |item| [item, align: \top]};
 

    // @FIXME Doesn't actually work
    // var blankSpace = [nil];
    ^
      // VLayout([toggleSlotButton, stretch: 2], wetness, [randomizeSectionButton, stretch: 1]), 
      [VLayout(*items), align: \top]
  }

  makePresetSection{

    var buttons = HLayout(*[

      // Add
      Button.new()
      .states_([["add"], ["add"]])
      .action_({|obj|  
        // var name = Date.getDate.stamp.asString;
        var name = Date.getDate.format("ofx%d%m%Y%H%M%S");
        preset.addSet(name);
        guiObjects[\main][\presetList].items = preset.settingNames
      }),

      // Load
      // Button.new()
      // .states_([["load"], ["load"]])
      // .action_({|obj| 
      //   // @TODO
      // }),

      // Save
      Button.new()
      .states_([["write"], ["write"]])
      .action_({|obj| 
        preset.writeSettings();
      }),

      // read
      Button.new()
      .states_([["read"], ["read"]])
      .action_({|obj| 
        preset.loadSettings;
        guiObjects[\main][\presetList].items = preset.settingNames
      }),

      // Open
      Button.new()
      .states_([["open"], ["open"]])
      .action_({|obj| 
        "not implemented yet".warn }),
      ]);

      var presetList = ListView.new()
      .selectionMode_(\single)
      .items_(preset.settingNames ? [])
      .action_{|obj| 
        var settingName = obj.item.asSymbol;
        preset.setCurr(settingName)
      };

      var title = StaticText.new().string_("presets:");

      guiObjects[\main] = guiObjects[\main] ? IdentityDictionary.new;
      guiObjects[\main][\presetList] = presetList;

      ^VLayout(title, buttons, presetList)
    }

  makeTransportSection{
    ^HLayout(*[

      // Play
      Button.new()
      .states_([["play"], ["play"]])
      .action_({|obj| if(obj.value == 1, { chain.play })}),

      // Stop
      Button.new()
      .states_([["stop"], ["stop"]])
      .action_({|obj| if(obj.value == 1, { chain.stop })}),

      // Clear
      Button.new()
      .states_([["clear"], ["clear"]])
      .action_({|obj| if(obj.value == 1, { chain.clear })}),

      // End
      Button.new()
      .states_([["end"], ["end"]])
      .action_({|obj| if(obj.value == 1, { chain.end })}),
    ])
  }

  makeGuiData {
    chain.slotNames.do{|slotName|
      var proxyValues = chain.keysValuesAt(slotName);

      // "Making gui data for slot %".format(slotName).postln;
      // if(proxyValues.isNil or: { proxyValues.isEmpty }, { "Nothing in proxyValues for %".format(slotName).warn });
      proxyValues.do{|pair|
        var key = pair[0];
        var val = pair[1];
        // "Adding %, % to guiData".format(key, val).postln;
        guiData[slotName][key] = val ?? 0
      }
    }
  }

  makeSkipjack {
    SkipJack.new(
      updateFunc: {

        if(this.isProxyInSync().not, { 
          // "Remaking gui".postln;
          this.makeGui;
        });

        if(this.checkSlotActivity(), { this.makeGui });

        this.checkAllActiveSlots()
      },  
      dt: 0.1,  
      stopTest: { window.isNil or: { window.isClosed } },  
      name: chain.key,  
    );
  }

  checkWetness{|slotName|
    var wetVal = chain.getWet(slotName);
    var guiVal = guiObjects[slotName][\wetnessSlider].value;

    if( wetVal != guiVal and: { wetVal.notNil },{ 
      guiObjects[slotName][\wetnessSlider].value_(wetVal)
    });

  }

  checkSlotActivity{
    var chainSlots = chain.slotsInUse;

    if(slotsInUse != chainSlots,{ 
      // "New slots became active. Old slots: %, new slots: %".format(slotsInUse, chainSlots).postln;
      slotsInUse = chain.slotsInUse.copy;
      ^true
    }, { ^false });
  }

  isProxyInSync{
    var status;

    // Did the slot chain setup change?
    if(
      slotNames != chain.slotNames, {
        ^false 
      }, { ^true });

      
    // Check if slotnames changed
    // Check if parameters changed
    // Check if specs changed

  }

  // Checks whether something is new in a slot (whether it was updated manually outside the gui) and if so updates the gui elements
  checkSlot {|sourceName|
    var proxyValues = chain.keysValuesAt(sourceName);

    // if(proxyValues.isNil, { "no proxy values for %".format(sourceName).warn });

    proxyValues.do{|pair|
      var key = pair[0];
      var proxyval = pair[1];
      var val = guiData[sourceName][key];
      var guiObject = guiObjects[sourceName][\parameterSliders][key];
      // "guiObject: %".format(~guiObjects[sourceName][key]).postln;
      // "sourcename: %, key: %, val: %".format(sourceName, key, val).postln;

      if(guiObject.isNil, { 
        // "Chain: No gui object for key % in slot %".format(key, sourceName).warn
      }, { 
        // Only have to update the gui - the other way around is always in sync because an action is called
        if(proxyval != val /*or: { val != guiObject[\valueLabel].value }*/, {
          // @FIXME: Redundancy - both gui actions and this do mapping/unmapping
          var spec = chain.getSpecForSourceAndParam(sourceName, key);
          var unmapped = spec.unmap(proxyval);
       
          // "val: %, proxyval: %".format(val, proxyval).postln;
          guiData[sourceName][key] = proxyval;
          guiObject.slider.value_(unmapped);
          guiObject.valueLabel.value_(proxyval);
        })

      })
    }
  }

  checkAllActiveSlots {
    slotNames.do{|slotName| 
      this.checkWetness(slotName);
      this.checkSlot(slotName) 
    }
  }

  slidersForSlot {|sourceName| 
    var sliders, params, objectDict;
    params = OFX_Chain.atSrcDict(sourceName).paramNames;
    objectDict = IdentityDictionary.new;

    guiData[sourceName] = guiData[sourceName] ?? IdentityDictionary.new;

    // if(params.isEmpty, { "No parameters found for %".format(sourceName).warn });

    sliders = params.collect{|paramName|
      var spec = chain.getSpecForSourceAndParam(sourceName, paramName) ;
      var rawVal= chain.getActiveParamValAt(sourceName, paramName) ?  0; //? chain.proxy.nodeMap() ? 0; 
      var currentWetness = spec.unmap(
        rawVal ? 0
      );

      var valueLabel = NumberBox.new()
      .value_(rawVal);

      var slider = Slider.new()
      .orientation_(\horizontal)
      .value_(currentWetness)
      .action_({|obj| 
        var val = spec.map(obj.value);

        valueLabel.value_(val);

        guiData[sourceName][paramName] = val;

        chain.proxy.set(paramName, val);
      });

      var label = StaticText.new()
      .string_(paramName);

      // Used for synchronising with the value of the proxy later on
      objectDict[paramName] = (valueLabel: valueLabel, slider: slider);

      VLayout(
        // label, 
        HLayout(
          [label, stretch: 2],
          [slider, stretch: 5], 
          [valueLabel, stretch: 1]
        )
      )
    };

    guiObjects[sourceName] = guiObjects[sourceName] ?? { IdentityDictionary.new };
    guiObjects[sourceName][\parameterSliders] = guiObjects[sourceName][\parameterSliders] ?? { IdentityDictionary.new };
    guiObjects[sourceName][\parameterSliders] = objectDict;

    ^sliders
  }
}

