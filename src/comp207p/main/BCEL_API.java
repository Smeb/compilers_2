package comp207p.main;

import org.apache.bcel.generic.BranchInstruction;
import org.apache.bcel.generic.ConstantPoolGen;
import org.apache.bcel.generic.LDC;
import org.apache.bcel.generic.LDC2_W;
import org.apache.bcel.generic.GOTO;
import org.apache.bcel.generic.IfInstruction;
import org.apache.bcel.generic.Instruction;
import org.apache.bcel.generic.InstructionList;
import org.apache.bcel.generic.InstructionHandle;
import org.apache.bcel.generic.TargetLostException;
import org.apache.bcel.generic.TypedInstruction;

public class BCEL_API {
  protected final static int SIG_B = 0;
  protected final static int SIG_S = 1;
  protected final static int SIG_I = 2;
  protected final static int SIG_J = 3;
  protected final static int SIG_F = 4;
  protected final static int SIG_D = 5;

  public static void fold_to_constant(ConstantPoolGen cpgen, InstructionHandle target, InstructionHandle signature, Number result){
    int sig = resolve_sig(cpgen, signature);
    int cp_index = cp_insert(cpgen, result, sig);
    switch(sig){
      case SIG_D: target.setInstruction(new LDC2_W(cp_index)); break;
      case SIG_J: target.setInstruction(new LDC2_W(cp_index)); break;
      default: target.setInstruction(new LDC(cp_index)); break;
    }
  }

  public static int cp_insert(ConstantPoolGen cpgen, Number value, int sig){
    switch(sig){
      case SIG_D: return cpgen.addDouble(value.doubleValue());
      case SIG_J: return cpgen.addLong(value.longValue());
      case SIG_F: return cpgen.addFloat(value.floatValue());
      default: return cpgen.addInteger(value.intValue());
    }
  }

  protected static int resolve_sig(ConstantPoolGen cpgen, InstructionHandle signature){
    Instruction i = signature.getInstruction();
    if(i instanceof TypedInstruction){
      String sig = ((TypedInstruction)i).getType(cpgen).getSignature();
      if(sig.equals("I")){
        return SIG_I;
      }
      else if(sig.equals("J")){
        return SIG_J;
      }
      else if(sig.equals("F")){
        return SIG_F;
      }
      else if(sig.equals("D")){
        return SIG_D;
      }
      else if(sig.equals("S")){
        return SIG_S;
      }
      else if(sig.equals("B")){
        return SIG_B;
      }
    }
    else if(i instanceof IfInstruction){
      return SIG_I;
    }
    throw new RuntimeException("Instruction - " + signature + " not typed - function should not be called");
  }

  protected static void remove_branch(ConstantPoolGen cpgen, InstructionList il, InstructionHandle load_h, InstructionHandle if_h, int result) throws TargetLostException{
    InstructionHandle start, end;
    if(result == 0){
      InstructionHandle goto_ih = ((BranchInstruction)if_h.getInstruction()).getTarget().getPrev();
      if(!(goto_ih.getInstruction() instanceof GOTO)){
        il.delete(load_h, if_h);
        return;
      }
      start = goto_ih.getNext();
      end = ((GOTO)goto_ih.getInstruction()).getTarget().getPrev();
      il.delete(goto_ih);
    }
    else{
      start = if_h.getNext();
      end = ((BranchInstruction)if_h.getInstruction()).getTarget().getPrev();
    }

    il.delete(load_h, if_h);
    il.delete(start, end);

  }
}
