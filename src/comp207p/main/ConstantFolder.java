package comp207p.main;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.Scanner;

import comp207p.main.BCEL_API;
import comp207p.main.ValueResolver;
import comp207p.main.utils.ValueLoadError;

import org.apache.bcel.classfile.ClassParser;
import org.apache.bcel.classfile.Code;
import org.apache.bcel.classfile.Constant;
import org.apache.bcel.classfile.ConstantInteger;
import org.apache.bcel.classfile.ConstantString;
import org.apache.bcel.classfile.ConstantUtf8;
import org.apache.bcel.classfile.ConstantPool;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.LineNumberTable;
import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.ArithmeticInstruction;
import org.apache.bcel.generic.ASTORE;
import org.apache.bcel.generic.ClassGen;
import org.apache.bcel.generic.ConstantPoolGen;
import org.apache.bcel.generic.ConstantPushInstruction;
import org.apache.bcel.generic.ConversionInstruction;
import org.apache.bcel.generic.LocalVariableInstruction;
import org.apache.bcel.generic.IfInstruction;
import org.apache.bcel.generic.Instruction;
import org.apache.bcel.generic.InstructionHandle;
import org.apache.bcel.generic.InstructionList;
import org.apache.bcel.generic.LDC;
import org.apache.bcel.generic.LDC_W;
import org.apache.bcel.generic.LDC2_W;
import org.apache.bcel.generic.LoadInstruction;
import org.apache.bcel.generic.MethodGen;
import org.apache.bcel.generic.StoreInstruction;
import org.apache.bcel.generic.TargetLostException;
import org.apache.bcel.generic.TypedInstruction;
import org.apache.bcel.util.InstructionFinder;



public class ConstantFolder
{
	ClassParser parser = null;
	ClassGen gen = null;
  final String load_instruction = "(ILOAD|LLOAD|FLOAD|DLOAD)"; // Ignore ALOAD - ALOAD loads object reference- ALOAD loads object references
  final String push_value = "(ConstantPushInstruction|"+ load_instruction + "|LDC|LDC2_W)";
  final String comparison_instructions = "(LCMP|DCMPL|DCMPG|FCMPL|FCMPG)? IfInstruction (ICONST GOTO ICONST)?";

  boolean _DEBUG = true;
  static boolean END_OPT = false;
  static boolean OPT_ALL = false;
  static boolean MODE_SELECTED = false;
  static int MODE = -1;
  final static int C_B_C = 0;
  final static int GRP_S = 1;
  static String grp_str = "";

	JavaClass original = null;
	JavaClass optimized = null;

	public ConstantFolder(String classFilePath)
	{
		try{
			this.parser = new ClassParser(classFilePath);
			this.original = this.parser.parse();
			this.gen = new ClassGen(this.original);
		} catch(IOException e){
			e.printStackTrace();
		}
	}

	public void write(String optimisedFilePath)
	{
    if(!MODE_SELECTED){
      Scanner scanner = new Scanner(System.in);
      System.out.println("================================================");
      System.out.println("Select run mode");
      do {
        System.out.println("0 - class by class, 1 - named groups");
        System.out.println("================================================");
        MODE = scanner.nextInt();
        scanner.nextLine();
      } while(MODE != C_B_C && MODE != GRP_S);
      if(MODE == GRP_S){
        System.out.println("================================================");
        System.out.println("Enter string to check on");
        System.out.println("================================================");
        grp_str = scanner.nextLine().toLowerCase();
      }
      MODE_SELECTED = true;
    }
    // TODO: Needs to be removed back to optimize
    this.optimize();
		try {
			FileOutputStream out = new FileOutputStream(new File(optimisedFilePath));
      this.optimized.dump(out);
		} catch (FileNotFoundException e) {
			// Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// Auto-generated catch block
			e.printStackTrace();
		}
	}


  private void optimise_method(ClassGen cgen, ConstantPoolGen cpgen, Method method){
    if(method == null){
      return;
    }

    // To manipulate the method we need a MethodGen, and an InstructionList

    InstructionList il = new InstructionList(method.getCode().getCode());

    if(_DEBUG){
      System.out.println("Method: " + method);
      System.out.println("================================================");
      System.out.println("Pre-optimisation instructions:");
      print_instructions(il);
      System.out.println("================================================");
    }

    MethodGen mgen = new MethodGen(
        method.getAccessFlags(),
        method.getReturnType(),
        method.getArgumentTypes(),
        null,
        method.getName(),
        cgen.getClassName(),
        il,
        cpgen);

    boolean optimised;
    do {
      optimised = false;
      optimised |= optimise_arithmetic(cpgen, il);  // All arithmetic instructions should be in the form loadX loadY op
      optimised |= optimise_comparisons(cpgen, il);  // All arithmetic instructions should be in the form loadX loadY op
      optimised |= optimise_negation(cpgen, il);
    } while(optimised);
    do {
      // post optimisation pass to remove excess conversions where
      // possible.
      optimised = false;
      optimised |= optimise_casts(cpgen, il);
    } while(optimised);

    if(_DEBUG){
      System.out.println("Optimised instructions:");
      print_instructions(il);
      System.out.println("================================================");
    }

    // Due to an unresolved bug with changing the instruction list of a
    // deep copied method it's easier to just create an entirely new
    // method
    il.setPositions(true);
    mgen.setMaxStack();
    mgen.setMaxLocals();
    cgen.setMajor(50);
    cgen.replaceMethod(method, mgen.getMethod());
  }

  private void purge_conversions(InstructionList il, InstructionHandle[] matches){
    // Delete conversions only for constants from the pool when the
    // conversion precedes an operation taking two stack inputs
    for(InstructionHandle h : matches){
      if(h.getInstruction() instanceof ConversionInstruction){
        try {
          il.delete(h);
        } catch (TargetLostException e){
          e.printStackTrace();
        }
      }
    }
  }

  private boolean optimise_negation(ConstantPoolGen cpgen, InstructionList il){
    boolean optimised = false;
    InstructionFinder f = new InstructionFinder(il);
    String negation_regex = push_value + " " + "(DNEG|FNEG|INEG|LNEG)";
    for(Iterator it = f.search(negation_regex); it.hasNext();){
      InstructionHandle[] matches = (InstructionHandle[]) it.next();
      if(_DEBUG){
        print_matches(matches);
      }
      Number left_v;
      try{
        left_v = ValueResolver.get_value(cpgen, matches[0], matches[1]);
      } catch(ValueLoadError e){
        System.out.println("Value could not be resolved - no folding");
        continue;
      }
      Number result = ValueResolver.resolve_negation(cpgen, left_v, matches[1]);
      BCEL_API.fold_to_constant(cpgen, matches[0], matches[1], result);
      try {
        il.delete(matches[1]);
      } catch (TargetLostException e){
        e.printStackTrace();
      }
      optimised = true;
    }
    return optimised;

  }

  private boolean optimise_casts(ConstantPoolGen cpgen, InstructionList il){
    boolean optimised = false;
    InstructionFinder f = new InstructionFinder(il);
    String comparison_regex = push_value + "(ConversionInstruction)+";
    InstructionHandle[] matches = null;
    for(Iterator it = f.search(comparison_regex); it.hasNext();){
      matches = (InstructionHandle[]) it.next();
      if(_DEBUG){
        print_matches(matches);
      }
      int end_index = matches.length;
      System.out.println(end_index);
      Number left_v;
      InstructionHandle sig = matches[end_index - 1].getNext();
      try {
        left_v = ValueResolver.get_value(cpgen, matches[0], sig);
      } catch(ValueLoadError e){
        System.out.println("Value could not be resolved - no folding");
        continue;
      }

      // The last instruction past the load instruction should be the
      // signature
      if(!(sig.getInstruction() instanceof TypedInstruction)){
        System.out.println("Conversion type could not be resolved");
        continue;
      }
      BCEL_API.fold_to_constant(cpgen, matches[0], sig, left_v);

      try {
        il.delete(matches[1], matches[end_index - 1]);
      } catch(TargetLostException e){
        e.printStackTrace();
      }
      optimised = true;
    }
    return optimised;
  }

  private boolean optimise_comparisons(ConstantPoolGen cpgen, InstructionList il){
    boolean optimised = false;
    InstructionFinder f = new InstructionFinder(il);
    String comparison_regex = push_value + " " + "(ConversionInstruction)*" +
                              push_value + " " + "(ConversionInstruction)*" + // arguably the second push value is optional
                              comparison_instructions;
    InstructionHandle[] matches = null;
    for(Iterator it = f.search(comparison_regex); it.hasNext();){
      matches = (InstructionHandle[]) it.next();
      if(_DEBUG){
        print_matches(matches);
      }

      InstructionHandle ih1 = matches[0];
      InstructionHandle ih2 = next_nonconversion(ih1);
      InstructionHandle ih3 = next_nonconversion(ih2);
      try{
        if(ih3.getInstruction() instanceof IfInstruction){
          optimise_comparison(cpgen, il, ih1, ih2, ih3);
        }
        else{
          InstructionHandle ih4 = next_nonconversion(ih3);
          optimise_comparison(cpgen, il, ih1, ih2, ih3, ih4);
        }
      } catch(ValueLoadError e){
        System.out.println(e.getMessage());
        continue;
      }

      optimised = false;
    }
    return optimised;
  }

  public void optimise_comparison(ConstantPoolGen cpgen, InstructionList il, InstructionHandle load_h1, InstructionHandle load_h2, InstructionHandle if_h) throws ValueLoadError{
    // Instructions in the sequence PUSH PUSH IF - these will be integer
    // based comparisons
    Number left_v, right_v;
    left_v = ValueResolver.get_value(cpgen, load_h1, if_h);
    right_v = ValueResolver.get_value(cpgen, load_h2, if_h);
    boolean result = ValueResolver.eval_comparison(left_v, right_v, if_h);
    System.out.println("Compared: " + left_v.intValue() + " " + right_v.intValue() + " result: " + result);
  }
  public void optimise_comparison(ConstantPoolGen cpgen, InstructionList il, InstructionHandle load_h1, InstructionHandle load_h2, InstructionHandle comp_h, InstructionHandle if_h) throws ValueLoadError{
    // Instructions in the sequence PUSH PUSH (NON INT CMP) IF - non
    // integer comparisons
    Number left_v, right_v;
    left_v = ValueResolver.get_value(cpgen, load_h1, comp_h);
    right_v = ValueResolver.get_value(cpgen, load_h2, comp_h);
    boolean result = ValueResolver.eval_comparison(left_v, right_v, comp_h);
    System.out.println("Compared: " + left_v.intValue() + " " + right_v.intValue() + " result: " + result);
  }

  private boolean is_int_comp(IfInstruction op) { // I believe this is superfluous now
    String op_s = op.getClass().getSimpleName();
    return op_s.contains("if_");
  }

  private boolean compare_op(Number l, Number r, IfInstruction op) throws RuntimeException {
    String op_s = op.getClass().getSimpleName();
    if(op_s.contains("EQ")){
      return l.doubleValue() == r.doubleValue();
    }
    else if(op_s.contains("GE")){
      return l.doubleValue() >= r.doubleValue();
    }
    else if(op_s.contains("GT")){
      return l.doubleValue() > r.doubleValue();
    }
    else if(op_s.contains("LE")){
      return l.doubleValue() <= r.doubleValue();
    }
    else if(op_s.contains("LT")){
      return l.doubleValue() < r.doubleValue();
    }
    else if(op_s.contains("NE")){
      return l.doubleValue() != r.doubleValue();
    }
    else {
      throw new RuntimeException("Operation: " + op.getClass() + " not recognized");
    }
  }

  private boolean optimise_arithmetic(ConstantPoolGen cpgen, InstructionList il){
    boolean optimised = false;
    // Since conversions are removed we should now have input in the
    // form push push op
    // 1. Find values in form push push operation
    // 2. Extract values from stack variables
    // 3. Find type of operation
    // 4. Compute new value of type of operation result
    // 5. Check if constant pool values used elsewhere, if not remove
    // them
    // 6. Remove pushes and operation from instruction list
    // 7. Add new push of constant pool value to instruction list
    // 8. Regenerate InstructionFinder f and repeat (new patterns may
    // have emerged).

    InstructionFinder f = new InstructionFinder(il);
    String arithmetic_regex = push_value + " " + "(ConversionInstruction)*" +
                              push_value + " " + "(ConversionInstruction)*" +
                              "ArithmeticInstruction";

    InstructionHandle[] matches = null;
    for(Iterator it = f.search(arithmetic_regex); it.hasNext();){
      matches = (InstructionHandle[]) it.next();
      if(_DEBUG){
        print_matches(matches);
      }

      Number left_v, right_v;
      InstructionHandle ih = next_nonconversion(matches[0]);
      InstructionHandle ih2 = next_nonconversion(ih);

      try {
        left_v = ValueResolver.get_value(cpgen, matches[0], ih2);
        right_v = ValueResolver.get_value(cpgen, ih, ih2);
      } catch (ValueLoadError e){
        System.out.println("Value could not be resolved - no folding");
        continue;
      }


      Number result = ValueResolver.resolve_arithmetic_op(cpgen, left_v, right_v, ih2);
      BCEL_API.fold_to_constant(cpgen, matches[0], ih2, result);

      // Delete unneeded instruction handles
      try {
        if(matches.length > 3){
          purge_conversions(il, matches);
        }
        il.delete(ih, ih2);
      } catch (TargetLostException e) {
        e.printStackTrace();
      }

      optimised = true;
      f.reread();
      it = f.search(arithmetic_regex);
    }
    return optimised;
  }

  public int class_by_class(){
      Scanner reader = new Scanner(System.in);
      System.out.println("================================================");
      System.out.println("Optimise class: '" + original.getClassName() + "' ?");
      System.out.println("2 --> optimise all; 1 --> yes; 0 --> no; -1 --> no further optimisations");
      System.out.println("================================================");
      return reader.nextInt();
  }

  public int check_class(){
    if(original.getClassName().toLowerCase().contains(grp_str)) return 1;
    return 0;
  }

  public void compile(ClassGen cgen){
    this.optimized = cgen.getJavaClass();
  }
	public void optimize()
  {
    ClassGen cgen = new ClassGen(original);
    int n = 0;

    if(!OPT_ALL){
      switch(MODE){
        case C_B_C: n = class_by_class();
                    break;
        case GRP_S: n = check_class();
      }
      if(n == 0 || n == -1 || END_OPT){
        if(n == -1){
          END_OPT = true;
        }
        compile(cgen);
        return;
      }
      if(n == 2){
        OPT_ALL = true;
      }
    }

		ConstantPoolGen cpgen = cgen.getConstantPool();
    ConstantPool cp = cpgen.getConstantPool();
    Constant[] constants = cp.getConstantPool();
    Method[] methods = cgen.getMethods();

    System.out.println("================================================");
    System.out.println("Class constant pool");
    for(Constant c : constants){
      // Debugging to print out constants that we care about
      if(c == null) continue;
      else if(c instanceof ConstantString) continue;
      else if(c instanceof ConstantUtf8) continue;
      else System.out.println(c);
    }
    System.out.println("================================================");

    for(Method m : methods){
      // Optimisation body should be in this submethod
      optimise_method(cgen, cpgen, m);
    }
    this.optimized = cgen.getJavaClass();
	}

  private InstructionHandle next_nonconversion(InstructionHandle ih){
    while((ih = ih.getNext()) != null){
      if(!(ih.getInstruction() instanceof ConversionInstruction)){
        return ih;
      }
    }
    throw new RuntimeException("Incorrectly handled conversion slide");
  }

  private void print_matches(InstructionHandle[] matches){
    System.out.println("Optimisable instructions found:");
    for(InstructionHandle h : matches){
      System.out.println(h);
    }
    System.out.println("================================================");
  }

  private void print_instructions(InstructionList il){
    InstructionHandle[] handles = il.getInstructionHandles();
    for(InstructionHandle h : handles){
      System.out.println(h);
    }
  }
}
