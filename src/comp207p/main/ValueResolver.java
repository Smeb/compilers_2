package comp207p.main;

import comp207p.main.utils.ValueLoadError;
import comp207p.main.BCEL_API;

import org.apache.bcel.generic.ASTORE;
import org.apache.bcel.generic.ArithmeticInstruction;
import org.apache.bcel.generic.BranchInstruction;
import org.apache.bcel.generic.ConstantPoolGen;
import org.apache.bcel.generic.ConstantPushInstruction;
import org.apache.bcel.generic.ConversionInstruction;
import org.apache.bcel.generic.IINC;
import org.apache.bcel.generic.IfInstruction;
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
import org.apache.bcel.generic.DCMPG;
import org.apache.bcel.generic.FCMPG;
import org.apache.bcel.generic.DCMPL;
import org.apache.bcel.generic.FCMPL;
import org.apache.bcel.generic.LCMP;

import org.apache.bcel.generic.IFLE;
import org.apache.bcel.generic.IFLT;
import org.apache.bcel.generic.IFGE;
import org.apache.bcel.generic.IFGT;
import org.apache.bcel.generic.IFEQ;
import org.apache.bcel.generic.IFNE;

import org.apache.bcel.generic.IF_ICMPLE;
import org.apache.bcel.generic.IF_ICMPLT;
import org.apache.bcel.generic.IF_ICMPGE;
import org.apache.bcel.generic.IF_ICMPGT;
import org.apache.bcel.generic.IF_ICMPEQ;
import org.apache.bcel.generic.IF_ICMPNE;

public class ValueResolver {

  protected static Number get_value(ConstantPoolGen cpgen, InstructionHandle handle, InstructionHandle sig) throws ValueLoadError{
    if(handle.getInstruction() instanceof LoadInstruction){
      return get_load_value(cpgen, handle, BCEL_API.resolve_sig(cpgen, sig));
    }
    // Else should be a constant value
    return get_constant_value(cpgen, handle.getInstruction());
  }

  private static Number get_load_value(ConstantPoolGen cpgen, InstructionHandle handle, int sig) throws ValueLoadError{
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
      throw new ValueLoadError("Assignment happens in branch - variable will not be loaded");
    }

    try {
      Number value = get_constant_value(cpgen, h.getInstruction());
      switch(sig){
        case BCEL_API.SIG_I: return value.intValue() + acc;
        case BCEL_API.SIG_J: return value.longValue() + acc;
        case BCEL_API.SIG_F: return value.floatValue() + acc;
        case BCEL_API.SIG_D: return value.doubleValue() + acc;
      }
    } catch (RuntimeException e){
      throw new ValueLoadError("Value of variable could not be resolved");
    }
    throw new ValueLoadError("Value of variable could not be resolved");
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
    InstructionHandle sub_h;
    while(h.getPrev() != null){
      // go to head of code
      h = h.getPrev();
    }

    while(h != null && h!= handle){
      if(h.getInstruction() instanceof BranchInstruction){
        sub_h = ((BranchInstruction)h.getInstruction()).getTarget();
        while(sub_h != null && sub_h != handle){
          sub_h = sub_h.getNext();
        }
        if(sub_h == null){
          return true;
        }
      }
      h = h.getNext();
    }
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

  protected static int eval_comparison(Number l, Number r, InstructionHandle cmp) throws ValueLoadError {
    if(r != null){
      l = new Integer(l.intValue() - r.intValue());
    }
    return eval_comparison(l.intValue(), cmp);
  }

  protected static int eval_comparison(Number l, Number r, InstructionHandle cmp, InstructionHandle if_h) throws ValueLoadError {
    int value;
    Instruction i = cmp.getInstruction();
    if(i instanceof DCMPG || i instanceof FCMPG){
      if(l.doubleValue() > r.doubleValue()) value = 1;
      else value = -1;
    }
    else if(i instanceof DCMPL || i instanceof FCMPL){
      if(l.doubleValue() < r.doubleValue()) value = -1;
      else value = 1;
    }
    else if(i instanceof LCMP){
      if(l.longValue() > r.longValue()) value = 1;
      else if(l.longValue() == r.longValue()) value = 0;
      else value = -1;
    }
    else {
      throw new ValueLoadError("Error, could not resolve signature preceding IfInstruction");
    }
    return eval_comparison(value, if_h);
  }

  private static int eval_comparison(int value, InstructionHandle sig) throws ValueLoadError {
    Instruction i = sig.getInstruction();
    if(i instanceof IF_ICMPEQ || i instanceof IFEQ){
      if(value == 0) return 1;
      return 0;
    }
    else if(i instanceof IF_ICMPGE || i instanceof IFGE){
      if(value >= 0) return 1;
      return 0;
    }
    else if(i instanceof IF_ICMPGT || i instanceof IFGT){
      if(value > 0) return 1;
      return 0;
    }
    else if(i instanceof IF_ICMPLE || i instanceof IFLE){
      if(value <= 0) return 1;
      return 0;
    }
    else if(i instanceof IF_ICMPLT || i instanceof IFLT){
      if(value < 0) return 1;
      return 0;
    }
    else if(i instanceof IF_ICMPNE || i instanceof IFNE){
      if(value != 0) return 1;
      return 0;
    }
    else {
      throw new ValueLoadError("Comparison is not of supported type");
    }
  }
  private static boolean eval_other_comparison(Number l, Number r, InstructionHandle sig){
    return true;
  }
}
