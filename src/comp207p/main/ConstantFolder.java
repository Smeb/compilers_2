package comp207p.main;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.Scanner;

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
import org.apache.bcel.generic.ClassGen;
import org.apache.bcel.generic.ConstantPoolGen;
import org.apache.bcel.generic.ConstantPushInstruction;
import org.apache.bcel.generic.ConversionInstruction;
import org.apache.bcel.generic.InstructionHandle;
import org.apache.bcel.generic.InstructionList;
import org.apache.bcel.generic.LDC;
import org.apache.bcel.generic.LDC2_W;
import org.apache.bcel.generic.MethodGen;
import org.apache.bcel.generic.TargetLostException;
import org.apache.bcel.util.InstructionFinder;



public class ConstantFolder
{
	ClassParser parser = null;
	ClassGen gen = null;
  final String constantPush = "(ConstantPushInstruction|LDC|LDC2_W)";

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

  private void optimize_method(ClassGen cgen, ConstantPoolGen cpgen, Method method){
    if(method == null){
      return;
    }


    // To manipulate the method we need a MethodGen, an InstructionList, and
    // an InstructionFinder

    MethodGen mGen = new MethodGen(method, cgen.getClassName(), cpgen);
    InstructionList il = new InstructionList(method.getCode().getCode());
    InstructionFinder f = new InstructionFinder(il);

    InstructionHandle[] handles = il.getInstructionHandles();
    System.out.println("================================================");
    System.out.println("Method instructions:");
    for(InstructionHandle h : handles){
      System.out.println(h);
    }
    System.out.println("================================================");
    simple_fold_optimisation(cpgen, f, il);
    handles = il.getInstructionHandles();
    System.out.println("================================================");
    System.out.println("Optimised instructions:");
    for(InstructionHandle h : handles){
      System.out.println(h);
    }
    System.out.println("================================================");
  }

  private void optimise_conversions(ConstantPoolGen cpgen, InstructionFinder f, InstructionList il){
    // Delete conversions only for constants from the pool
    String conversionRegExp = constantPush + " " + "(ConversionInstruction)*" + " " +
                              constantPush + " " + "(ConversionInstruction)*" + " " +
                              "ArithmeticInstruction";
    Iterator it = f.search(conversionRegExp);
    System.out.println(conversionRegExp);
    while(it.hasNext()){
      InstructionHandle[] matches = (InstructionHandle[]) it.next();
      for(InstructionHandle h : matches){
          if(h.getInstruction() instanceof ConversionInstruction){
            try {
              il.delete(h);
              System.out.println("Deleted conversion");
            } catch (TargetLostException e){
              e.printStackTrace();
            }
          }
      }
    }
  }

  private void optimise_arithmetic(ConstantPoolGen cpgen, InstructionFinder f, InstructionList il){
    // Conversions optimised earlier
    String simpleRegExp = constantPush + " " +
                          constantPush + " " +
                          "ArithmeticInstruction";
    Iterator it = f.search(simpleRegExp);
    int i = 0;
    while(it.hasNext()){
      InstructionHandle[] matches = (InstructionHandle[]) it.next();
      System.out.println("Optimisable instructions found:");
      for(InstructionHandle h : matches){
        System.out.println(h);
      }

      LDC left = (LDC) matches[0].getInstruction();
      LDC right = (LDC) matches[1].getInstruction();
      ArithmeticInstruction op = (ArithmeticInstruction) matches[2].getInstruction();

      System.out.format("%s:%s %s %s:%s\n", left.getType(cpgen), left.getValue(cpgen),
          op, right.getType(cpgen), right.getValue(cpgen));
      String opSig = op.getType(cpgen).getSignature();
      if(opSig.equals("D")){
        //op is double
        ;
      }
      else if(opSig.equals("F")){
        //op is float
        ;
      }
      else if(opSig.equals("L")){
        //op is long
        ;
      }
      else if(opSig.equals("I")){
        //op is int
        ;
      }

      // Need to resolve type of operation
      //

      if(i++ == 10) break;
    }
  }

  private void simple_fold_optimisation(ConstantPoolGen cpgen, InstructionFinder f, InstructionList il){
    // Constant folding for int, long, float, double in bytecode
    // constant pool. Provided an unoptimised constant pool

    // https://commons.apache.org/proper/commons-bcel/apidocs/org/apache/bcel/generic/Instruction.html
    // for list of all possible insructions to match against
    // Regexp is case insensitive so explicit class names used, also {2}
    // does not work for repeating regex
    //
    //Regex explanation:
    // ConstantPushInstruction - Push a literal onto the stack (byte, short)
    // LDC                     - Push item from constant pool
    // LDC2_W                  - Push long or double from constant pool
    // ArithmeticInstruction   -
    //
    // In Java: Double > Flaot > Long > Int, so that
    // byte -> byte -> int
    // int -> int -> int
    // double -> byte -> double
    // etc.

    System.out.println("================================================");
    System.out.println("Scanning for optimisations");
    System.out.println("================================================");
    optimise_conversions(cpgen, f, il);
    // Assumption: conversion could happen from loaded constants
  }

  private void constant_variable_folding(){
    // Optimise uses of local variables of type int, long, float,
    // double, whose value does not change throughout the scope of the
    // method -> after declaration, variable is not reassigned. Need to
    // propogate THROUGHOUT the method.
    return;
  }
  private void dynamic_variable_folding(){
    // Optimise uses of local variables of int, long,float, and double,
    // whose value will be reassinged with a different constant number
    // during the scope of the method. Value needs to propagate, but for
    // specific intervals. (assignment <-> reassignment)
    return;
  }

	public void optimize()
	{
    Scanner reader = new Scanner(System.in);
    System.out.println("================================================");
    System.out.println("Optimize class: '" + original.getClassName() + "' ?");
    System.out.println("1 --> yes; 0 --> no");
    System.out.println("================================================");
    int n = reader.nextInt();
    if(n == 0){
      this.optimized = gen.getJavaClass();
      return;
    }


		ClassGen cgen = new ClassGen(original);
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
      System.out.println("================================================");
      System.out.println("Optimize method: '" + m + "' ?");
      System.out.println("================================================");
      n = reader.nextInt();
      if(n == 0) continue;
      optimize_method(cgen, cpgen, m);
    }


		this.optimized = gen.getJavaClass();
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
}
