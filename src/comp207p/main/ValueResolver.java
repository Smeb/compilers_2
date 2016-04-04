package comp207p.main;

import comp207p.main.utils.ValueLoadError;

import org.apache.bcel.generic.ASTORE;
import org.apache.bcel.generic.BranchInstruction;
import org.apache.bcel.generic.ConstantPoolGen;
import org.apache.bcel.generic.ConstantPushInstruction;
import org.apache.bcel.generic.ConversionInstruction;
import org.apache.bcel.generic.GotoInstruction;
import org.apache.bcel.generic.IINC;
import org.apache.bcel.generic.Instruction;
import org.apache.bcel.generic.InstructionList;
import org.apache.bcel.generic.InstructionHandle;
import org.apache.bcel.generic.LDC;
import org.apache.bcel.generic.LDC_W;
import org.apache.bcel.generic.LDC2_W;
import org.apache.bcel.generic.LocalVariableInstruction;
import org.apache.bcel.generic.LoadInstruction;
import org.apache.bcel.generic.StoreInstruction;
import org.apache.bcel.generic.TargetLostException;

public class ValueResolver {

  protected static Number get_value(ConstantPoolGen cpgen, InstructionHandle handle) throws ValueLoadError{
    if(handle.getInstruction() instanceof LoadInstruction){
      return get_load_value(cpgen, handle);
    }
    // Else should be a constant value
    return get_constant_value(cpgen, handle.getInstruction());
  }

  private static Number get_load_value(ConstantPoolGen cpgen, InstructionHandle handle) throws ValueLoadError{
    // if load instruction found, need to find and resolve the most
    // recent store instruction to the variable - we are not folding
    // variable stores so this should always be fine

    // Identify target variable
    //
    // TODO: Loading from conversions - > possible optimisation of those
    // conversions
    int index = ((LocalVariableInstruction) handle.getInstruction()).getIndex();
    int acc = 0;
    InstructionHandle h = handle;

    while((h = h.getPrev()) != null){
      if(h.getInstruction() instanceof IINC && ((IINC)h.getInstruction()).getIndex() == index){
        acc += ((IINC)h.getInstruction()).getIncrement();
      }
      else if(h.getInstruction() instanceof StoreInstruction &&
          ((StoreInstruction)h.getInstruction()).getIndex() == index){
        break;
          }
    }
    while((h = h.getPrev()) != null){
      if(!(h.getInstruction() instanceof ConversionInstruction)){
        break;
      }
    }

    if(h.getInstruction() instanceof ASTORE){
      throw new ValueLoadError("Array storage is not in the scope of coursework");
    }
    try {
      return get_constant_value(cpgen, h.getInstruction()).doubleValue() + acc;
    } catch (RuntimeException e){
      throw new ValueLoadError("Value of variable could not be resolved");
    }
  }

  private static Number get_constant_value(ConstantPoolGen cpgen, Instruction instruction){
    if(instruction instanceof ConstantPushInstruction){
      return (Number) ((ConstantPushInstruction) instruction).getValue();
    }
    else if(instruction instanceof LDC){
      return (Number) ((LDC) instruction).getValue(cpgen);
    }
    else if(instruction instanceof LDC2_W){
      return (Number) ((LDC2_W) instruction).getValue(cpgen);
    }
    else{
      throw new RuntimeException("Instruction: " + instruction + " not a constant push instruction");
    }
  }

  private static boolean validate_load(InstructionHandle h){
    // Look forwards to check not in loop
    InstructionHandle comp = h;
    while(comp.getNext() != null){
      if(comp.getInstruction() instanceof GotoInstruction){
        Instruction prev = comp.getPrev().getInstruction();
        if(prev instanceof IINC || prev instanceof StoreInstruction){
          if(((BranchInstruction) comp.getInstruction()).getTarget().getInstruction().equals(h.getInstruction())){
            return true;
          }
        }
      }
    }
    return false;
  }
}
