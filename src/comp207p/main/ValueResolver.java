package comp207p.main;

import comp207p.main.utils.ValueLoadError;

import org.apache.bcel.generic.ASTORE;
import org.apache.bcel.generic.ArithmeticInstruction;
import org.apache.bcel.generic.BranchInstruction;
import org.apache.bcel.generic.ConstantPoolGen;
import org.apache.bcel.generic.ConstantPushInstruction;
import org.apache.bcel.generic.ConversionInstruction;
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
    if(in_loop(h)){
      throw new ValueLoadError("Assignment happens in loop - variable will not be loaded");
    }
    if(in_conditional_branch(h)){
      throw new ValueLoadError("Assignment happens in loop - variable will not be loaded");
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

  //TODO: Need to ensure to check that skippable instructions are not
  //evaluated
  private static boolean in_loop(InstructionHandle handle){
    InstructionHandle h = handle;
    InstructionHandle sub_h;
    while((h = h.getNext()) != null){
      if(h.getInstruction() instanceof BranchInstruction){
        sub_h = ((BranchInstruction)h.getInstruction()).getTarget();
        while(sub_h != null && sub_h!= h){
          if(sub_h == handle){
            return true;
          }
          sub_h = sub_h.getNext();
        }
      }
    }
    return false;
  }

  private static boolean in_conditional_branch(InstructionHandle handle){
    // Should take as input the store_instruction -> there should be
    // instructions beforehand

    InstructionHandle h = handle;
    InstructionHandle sub_h = h;
    while(h.getPrev() != null){
      // go to head of code
      h = h.getPrev();
    }

    do {
      if(h.getInstruction() instanceof BranchInstruction){
        sub_h = ((BranchInstruction)h.getInstruction()).getTarget();
        while(sub_h != null && sub_h != handle){
          sub_h = sub_h.getNext();
          System.out.println(sub_h);
        }
        if(sub_h == null){
          return true;
        }
      }
      h = h.getNext();
    } while(h != null && h!= handle);
    return false;
  }

  protected static Number resolve_negation(ConstantPoolGen cpgen, Number l, InstructionHandle signature){
    int sig = BCEL_API.resolve_sig(cpgen, signature);
    switch(sig){
      case BCEL_API.SIG_D: return new Double(-1 * l.doubleValue());
      case BCEL_API.SIG_F: return new Double(-1 * l.doubleValue());
      default: return new Long(-1 * l.longValue());
    }
  }


  protected static Number resolve_arithmetic_op(ConstantPoolGen cpgen, Number l, Number r, InstructionHandle oph) throws RuntimeException {
    ArithmeticInstruction op = (ArithmeticInstruction)oph.getInstruction();
    int length = op.getClass().getSimpleName().length();
    String op_s = op.getClass().getSimpleName().substring(1, length);
    String sig = op.getType(cpgen).getSignature();
    if(sig.equals("F") || sig.equals("D")){
      if(op_s.equals("ADD")){
        return l.doubleValue() + r.doubleValue();
      }
      else if(op_s.equals("SUB")){
        return l.doubleValue() - r.doubleValue();
      }
      else if(op_s.equals("MUL")){
        return l.doubleValue() * r.doubleValue();
      }
      else if(op_s.equals("DIV")){
        return l.doubleValue() / r.doubleValue();
      }
      else if(op_s.equals("REM")){
        return l.doubleValue() % r.doubleValue();
      }
      else {
        throw new RuntimeException("Operation: " + op.getClass() + " not recognized");
      }
    }
    else {
      if(op_s.equals("ADD")){
        return l.longValue() + r.longValue();
      }
      else if(op_s.equals("SUB")){
        return l.longValue() - r.longValue();
      }
      else if(op_s.equals("MUL")){
        return l.longValue() * r.longValue();
      }
      else if(op_s.equals("DIV")){
        return l.longValue() / r.longValue();
      }
      else if(op_s.equals("REM")){
        return l.longValue() % r.longValue();
      }
      else if(op_s.equals("OR")){
        return new Long(l.longValue() | r.longValue());
      }
      else if(op_s.equals("XOR")){
        return new Long(l.longValue() ^ r.longValue());
      }
      else if(op_s.equals("AND")){
        return new Long(l.longValue() & r.longValue());
      }
      else if(op_s.equals("SHL")){
        return new Long(l.longValue() << r.longValue());
      }
      else if(op_s.equals("SHR")){
        return new Long(l.longValue() >> r.longValue());
      }
      else {
        throw new RuntimeException("Operation: " + op.getClass() + " not recognized");
      }
    }
  }


}
