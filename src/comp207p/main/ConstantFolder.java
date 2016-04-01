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
import org.apache.bcel.generic.ClassGen;
import org.apache.bcel.generic.ConstantPoolGen;
import org.apache.bcel.generic.InstructionHandle;
import org.apache.bcel.generic.InstructionList;
import org.apache.bcel.util.InstructionFinder;
import org.apache.bcel.generic.MethodGen;
import org.apache.bcel.generic.TargetLostException;



public class ConstantFolder
{
	ClassParser parser = null;
	ClassGen gen = null;

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

  public void optimize_method(ClassGen cgen, ConstantPoolGen cpgen, Method method){
    Code methodCode = method.getCode();
    if(methodCode != null){
      System.out.println("================================================");
      System.out.println(methodCode);
      System.out.println("================================================");
    }

    // Initialise a new MethodGen using the method as the prototype
    MethodGen mGen = new MethodGen(method, cgen.getClassName(), cpgen);
    InstructionList il = new InstructionList(methodCode.getCode());
    InstructionFinder ifind = new InstructionFinder(il);
  }

	public void optimize()
	{
    Scanner reader = new Scanner(System.in);
    System.out.println("================================================");
    System.out.println("Optimize class: '" + original.getClassName() + "' ?");
    System.out.println("0 --> no");
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
      System.out.println("0 --> no");
      System.out.println("================================================");
      n = reader.nextInt();
      if(n == 0) break;
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
