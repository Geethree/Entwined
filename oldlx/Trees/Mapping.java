import java.util.List;

import heronarts.lx.LX;
import heronarts.lx.color.LXColor;
import heronarts.lx.modulator.SinLFO;
import heronarts.lx.parameter.BooleanParameter;
import heronarts.lx.parameter.DiscreteParameter;

class MappingTool extends Effect {

  final List<CubeConfig> cubeConfig;
  final List<ShrubCubeConfig> shrubCubeConfig;

  final SinLFO strobe = new SinLFO(20, 100, 1000);
  
  final DiscreteParameter ipIndex;
  final DiscreteParameter outputIndex;
  final BooleanParameter showBlanks = new BooleanParameter("BLANKS", false);
  final DiscreteParameter shrubIpIndex;
  final DiscreteParameter shrubOutputIndex;
  final Object[] ipList;
  final Object[] shrubIpList;

  MappingTool(LX lx, List<CubeConfig> cubeConfig, List<ShrubCubeConfig> shrubCubeConfig) {
    super(lx);
    this.cubeConfig = cubeConfig;
    this.shrubCubeConfig = shrubCubeConfig;
    this.ipList = model.ipMap.keySet().toArray();
    this.shrubIpList = model.shrubIpMap.keySet().toArray();
    ipIndex = new DiscreteParameter("IP", ipList.length);
    outputIndex = new DiscreteParameter("POS", 16);
    shrubIpIndex = new DiscreteParameter("IP", shrubIpList.length);
    shrubOutputIndex = new DiscreteParameter("POS", 16);
    addModulator(strobe).start();
    addLayer(new MappingLayer());
  }

  public void run(double deltaMs) {
  }

  Cube getCube(){
    return model.ipMap.get(this.ipList[ipIndex.getValuei()])[outputIndex.getValuei()];
  }

  CubeConfig getConfig(){
    return getCube().config;
  }
  
  ShrubCube getShrubCube(){
    return ((ShrubModel) shrubModel).shrubIpMap.get(this.shrubIpList[shrubIpIndex.getValuei()])[shrubOutputIndex.getValuei()];
  }

  ShrubCubeConfig getShrubConfig(){
    return getShrubCube().config;
  }
  
  class MappingLayer extends Layer {
    
    MappingLayer() {
      super(MappingTool.this.lx);
    }
    
    public void run(double deltaMs) {
      if (isEnabled()) {
        blendColor(getCube().index, lx.hsb(0, 0, strobe.getValuef()), LXColor.Blend.ADD);
        blendColor(getShrubCube().index, lx.hsb(0, 0, strobe.getValuef()), LXColor.Blend.ADD);
      }
    }
  }
}
