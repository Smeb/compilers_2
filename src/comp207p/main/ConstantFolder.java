package comp207p.main;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.Scanner;

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
import org.apache.bcel.util.InstructionFinder;



public class ConstantFolder
{
	ClassParser parser = null;
	ClassGen gen = null;
  final String load_instruction = "(ILOAD|LLOAD|FLOAD|DLOAD)"; // Ignore ALOAD - ALOAD loads object reference- ALOAD loads object references
  final String push_value = "(ConstantPushInstruction|"+ load_instruction + "|LDC|LDC2_W)";
  final String comparison_instructions = "(LCMP|DCMPL|DCMPG|FCMPL|FCMPG) (IfInstruction ICONST GOTO ICONST)";

  boolean _DEBUG = true;
  static boolean END_OPT = false;

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
      //optimised |= optimise_comparisons(cpgen, il);  // All arithmetic instructions should be in the form loadX loadY op
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

  private void optimise_conversions(InstructionList il, InstructionHandle[] matches){
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

  private boolean optimise_comparisons(ConstantPoolGen cpgen, InstructionList il){
    boolean optimised = false;
    InstructionFinder f = new InstructionFinder(il);
    String comparison_regex = push_value +
                              push_value +
                              comparison_instructions;
    InstructionHandle[] matches = null;
    for(Iterator it = f.search(comparison_regex); it.hasNext();){
      matches = (InstructionHandle[]) it.next();
      if(_DEBUG){
        print_matches(matches);
      }
    }
    return optimised;
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
      InstructionHandle ih = matches[1];
      try {
        left_v = ValueResolver.get_value(cpgen, matches[0]);
        while(ih.getInstruction() instanceof ConversionInstruction){
          ih = ih.getNext();
        }
        right_v = ValueResolver.get_value(cpgen, ih);
      } catch (ValueLoadError e){
        System.out.println("Value could not be resolved - no folding");
        continue;
      }

      InstructionHandle ih2 = ih.getNext();
      while(ih2.getInstruction() instanceof ConversionInstruction){
        ih2 = ih2.getNext();
      }

      // Once values are loaded we can then optimise conversions that
      // appear in the load sequence. We do it after load since load can
      // fail


      ArithmeticInstruction op = (ArithmeticInstruction) ih2.getInstruction();

      Double result = ValueResolver.resolve_arithmetic_op(left_v, right_v, op);
      String op_sig = op.getType(cpgen).getSignature();
      System.out.println(left_v.doubleValue());
      System.out.println(right_v.doubleValue());
      System.out.println(result);

      // Change instruction handle to result type
      if(op_sig.equals("D"))
        matches[0].setInstruction(new LDC2_W(cpgen.addDouble(result)));
      else if(op_sig.equals("F"))
        matches[0].setInstruction(new LDC(cpgen.addFloat(result.floatValue())));
      else if(op_sig.equals("J"))
        matches[0].setInstruction(new LDC2_W(cpgen.addLong(result.longValue())));
      else if(op_sig.equals("I"))
        matches[0].setInstruction(new LDC(cpgen.addInteger(result.intValue())));

      // Delete unneeded instruction handles
      try {
        if(matches.length > 3){
          optimise_conversions(il, matches);
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

	public void optimize()
	{
		ClassGen cgen = new ClassGen(original);
    if(END_OPT){
      this.optimized = cgen.getJavaClass();
      return;
    }
    Scanner reader = new Scanner(System.in);
    System.out.println("================================================");
    System.out.println("Optimise class: '" + original.getClassName() + "' ?");
    System.out.println("1 --> yes; 0 --> no; -1 --> no further optimisations");
    System.out.println("================================================");
    int n = reader.nextInt();
    if(n == 0 || n == -1){
      if(n == -1){
        END_OPT = true;
      }
      this.optimized = cgen.getJavaClass();
      return;
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
