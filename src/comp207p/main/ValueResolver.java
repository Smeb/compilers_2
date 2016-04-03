package comp207p.main;

import comp207p.main.utils.ValueLoadError;
import org.apache.bcel.generic.ASTORE;
import org.apache.bcel.generic.BranchInstruction;
import org.apache.bcel.generic.ConstantPoolGen;
import org.apache.bcel.generic.ConstantPushInstruction;
import org.apache.bcel.generic.GotoInstruction;
import org.apache.bcel.generic.Instruction;
import org.apache.bcel.generic.InstructionHandle;
import org.apache.bcel.generic.LDC;
import org.apache.bcel.generic.LDC_W;
import org.apache.bcel.generic.LDC2_W;
import org.apache.bcel.generic.LocalVariableInstruction;
import org.apache.bcel.generic.LoadInstruction;
import org.apache.bcel.generic.StoreInstruction;

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
    int index = ((LocalVariableInstruction) handle.getInstruction()).getIndex();
    InstructionHandle store_handle = find_store_handle(handle, index);
    Instruction store_instruction = store_handle.getInstruction();
    if(store_instruction instanceof ASTORE){
      throw new RuntimeException("Array storage and realisation not in the scope of these optimisations");
    }
    try {
      return get_constant_value(cpgen, store_handle.getPrev().getInstruction());
    } catch (RuntimeException e){
      throw new ValueLoadError("Value of variable could not be resolved");
    }
  }

  private static InstructionHandle find_store_handle(InstructionHandle h, int index){
    while((h = h.getPrev()) != null){
      if(h.getInstruction() instanceof StoreInstruction &&
          ((StoreInstruction)h.getInstruction()).getIndex() == index){
        return h;
        }
      }
    throw new RuntimeException("Found dangling load instruction without store");
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
    InstructionHandle comp = h;
    while(comp.getPrev() != null){
      if(comp.getInstruction() instanceof GotoInstruction &&
          ((BranchInstruction)comp.getInstruction()).getTarget().getInstruction().equals(h.getInstruction()))
        return true;
    }
    return false;
  }
}
