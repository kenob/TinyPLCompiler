import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Queue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/* 		OO PARSER AND BYTE-CODE GENERATOR FOR TINY PL
 
Grammar for TinyPL (using EBNF notation) is as follows:

 program ->  decls stmts end
 decls   ->  int idlist ;
 idlist  ->  id { , id } 
 stmts   ->  stmt [ stmts ]
 cmpdstmt->  '{' stmts '}'
 stmt    ->  assign | cond | loop
 assign  ->  id = expr ;
 cond    ->  if '(' rexp ')' cmpdstmt [ else cmpdstmt ]
 loop    ->  while '(' rexp ')' cmpdstmt  
 rexp    ->  expr (< | > | =) expr
 expr    ->  term   [ (+ | -) expr ]
 term    ->  factor [ (* | /) term ]
 factor  ->  int_lit | id | '(' expr ')'
 
Lexical:   id is a single character; 
	      int_lit is an unsigned integer;
		 equality operator is =, not ==

Sample Program: Factorial
 
int n, i, f;
n = 4;
i = 1;
f = 1;
while (i < n) {
  i = i + 1;
  f= f * i;
}
end

   Sample Program:  GCD
   
int x, y;
x = 121;
y = 132;
while (x != y) {
  if (x > y) 
       { x = x - y; }
  else { y = y - x; }
}
end

 */

public class Parser {
	static HashMap<Character, Integer> id_map = new HashMap<Character, Integer>();
	static HashMap<Character, Integer> id_value_map = new HashMap<Character, Integer>();
	static int id = 0;
	public static void main(String[] args)  {
		System.out.println("Enter program and terminate with 'end'!\n");
		Lexer.lex();
		Code.setCodes();
		Code.bytesToAdd();
		Program p = new Program();
		System.out.println();
		System.out.println();
		System.out.println("JAVA BYTE CODE BELOW: -->");
		System.out.println();
		Code.output();
	}
}

class Program {
	Decls d;
	Stmts s;
	public Program(){
			 d = new Decls();
			 Lexer.lex();
			 s = new Stmts();
			 Code.out.add("return");
	}
}

class Decls {
	Idlist idl;
	public Decls(){
		if(Lexer.nextToken == Token.KEY_INT){
			Lexer.lex();
			idl = new Idlist();	
		}
	}
}

class Idlist {
	public Idlist(){
		while(Lexer.nextToken != Token.SEMICOLON)
		{
			if (Lexer.nextToken == Token.COMMA)
			{
				Lexer.lex();
				
			} else if (Lexer.nextToken == Token.ID) {
				Parser.id_map.put(Lexer.ident, Parser.id++);
				Parser.id_value_map.put(Lexer.ident, 0);
				Lexer.lex();
			} else {
				break;
			}
		}
	}
}

class Stmt {
	Assign a;
	Cond c;
	Loop l;
	
	public Stmt(){
		if(Lexer.nextToken == Token.KEY_WHILE){
			Lexer.lex();	// now in '(' (left brace)
			l = new Loop();
		} else if (Lexer.nextToken == Token.KEY_IF){
			Lexer.lex();	// now in '(' (left brace)
			c = new Cond();
		} else if (Lexer.nextToken == Token.ID) {
			Lexer.lex();	// skipping over equality
			a = new Assign();
			
		}
		
	}
	 
} 

class Stmts {
	Stmts st;
	Stmt s;
	public Stmts()
	 {	
		 s = new Stmt();
		 if (Lexer.nextToken == Token.RIGHT_BRACE){
			 return;
		 }
		 if (Lexer.nextToken != Token.KEY_END)
		 {	
			st = new Stmts();
		 }
		 return;
	 }
}

class Assign {
	static int resultOfFactor;
	static int resultOfTerm = 1;
	static int resultOfExpr = 0;
	static Boolean firstTermCovered = false;
	static Boolean firstFactorCovered = false;
	static char assignment;
	Expr e;
	
	public Assign(){
		assignment = Lexer.ident;
		Lexer.lex(); // moving over to first part of the expression
		e = new Expr();
		Lexer.lex();
		Parser.id_value_map.put(assignment, Assign.resultOfExpr);
		int storeID = Parser.id_map.get(assignment);
		Code.out.add("istore_" + storeID);
		Code.bytePosition += Code.getCost("istore"); 
		
		firstTermCovered = false;
		firstFactorCovered = false;
		resultOfTerm = 1;
		resultOfExpr = 0;
		
	}
	
	
}

class Cond {
	Rexpr rexp;
	Cmpdstmt c;
	 public Cond(){
		Lexer.lex();	// LHS of Rexpr
		rexp = new Rexpr();	// evaluate Rexpr
		Lexer.lex();	// goes to start of compound statement
		Lexer.lex();	// goes to first part of the compound statement
		int listinitialSize = Code.out.size() - 1;
		c = new Cmpdstmt();
		if (Lexer.nextToken == Token.KEY_ELSE){
			Code.out.add("goto");
			Code.bytePosition += Code.getCost("goto");
			Code.out.set(listinitialSize, Code.out.get(listinitialSize) + " " + Code.bytePosition);
			int gotoPosition = Code.out.size() - 1;
			Lexer.lex();	// goes to start of else compound statement
			Lexer.lex();	// goes to first part of else compound statement
			c = new Cmpdstmt();
			Code.out.set(gotoPosition, Code.out.get(gotoPosition) + " " + Code.bytePosition);
			
			
		}
		else{
			Code.out.set(listinitialSize, Code.out.get(listinitialSize) + " " + Code.bytePosition);
		}
		
		
		
	 }
}

class Loop {
	Rexpr rexp;
	Cmpdstmt cmp;
	 public Loop(){
		Lexer.lex(); // LHS of Rexpr
		int gotoWhilePos = Code.bytePosition;
		rexp = new Rexpr(); // evaluate Rexpr
		Lexer.lex(); // goes to start of compound statement
		Lexer.lex(); // goes to first part of the compound statement
		int listWhilePos = Code.out.size() - 1;
		cmp = new Cmpdstmt();
		Code.out.add("goto" + " " + gotoWhilePos);
		Code.bytePosition += Code.getCost("goto");
		Code.out.set(listWhilePos, Code.out.get(listWhilePos) + " "
					+ Code.bytePosition);
		
	 }
}

class Cmpdstmt {
	Stmts st;
	 public Cmpdstmt(){
		 st = new Stmts();
		 Lexer.lex();	// come to end of compound statement '}'
	 }
}

class Rexpr {
	Expr e1;
	Expr e2;
	 static Queue<String> queue = new LinkedList<String>();
	 static char rexprComparator;
	 public Rexpr(){
		 e1 = new Expr(); // evaluated first expression
		 int oper = Lexer.nextToken;
		 Lexer.lex(); // goes to RHS of Rexpr
		 e2 = new Expr();	// reached RIGHT PAREN of Rexpr
		 switch (oper){
		 
		 case 2:
			Code.out.add("if_icmpeq");
			Code.bytePosition += Code.getCost("if_icmpeq");
			break;
		 
		 case 7:
		    Code.out.add("if_icmpne");
			Code.bytePosition += Code.getCost("if_icmpne"); 
			break;
		 
		 case 8:
			Code.out.add("if_icmple");
			Code.bytePosition += Code.getCost("if_icmple"); 
			break;
		 
		 case 9:
			Code.out.add("if_icmpge");	
			Code.bytePosition += Code.getCost("if_icmpge"); 
			break;
			 
		 }
		 
		 
	 }
}

class Expr {  
	Term t;
	Expr e;
	char op;

	public Expr() {
		t = new Term();
		
		if (Assign.firstTermCovered == false) {
			Assign.resultOfExpr = Assign.resultOfTerm;
			Assign.firstTermCovered = true;
		}
		
		if (Lexer.nextToken == Token.ADD_OP || Lexer.nextToken == Token.SUB_OP) {
			op = Lexer.nextChar;
			int termresult = Assign.resultOfTerm;
			Lexer.lex();
			e = new Expr();
			
			switch(op){
			case '+':
				Assign.resultOfExpr = termresult + Assign.resultOfExpr;
				
				break;
			case '-':
				Assign.resultOfExpr = termresult - Assign.resultOfExpr;
				break;
			default:
				break;
			}
			
			Code.out.add(Code.getCode(op));
			Code.bytePosition += Code.getCost(Code.getCode(op));
		} else if (Lexer.nextToken != Token.ADD_OP && Lexer.nextToken != Token.SUB_OP){
			Assign.resultOfExpr = Assign.resultOfTerm;
		}
		
	}
}

class Term {  
	Factor f;
	Term t;
	char op;

	public Term() {
		f = new Factor();
		
		if (Assign.firstFactorCovered == false) {
			Assign.resultOfTerm = Assign.resultOfFactor;
			Assign.firstFactorCovered = true;
		}
		
		if (Lexer.nextToken == Token.MULT_OP || Lexer.nextToken == Token.DIV_OP) {
			op = Lexer.nextChar;
			int factresult = Assign.resultOfFactor;
			Lexer.lex();
			t = new Term();
			
			switch(op){
			case '*':
				Assign.resultOfTerm = factresult * Assign.resultOfTerm;
				break;
			case '/':
				Assign.resultOfTerm = factresult / Assign.resultOfTerm;
				break;
			default:
				break;
			}
			
			Code.out.add(Code.getCode(op));
			Code.bytePosition += Code.getCost(Code.getCode(op));
			
		} else if (Lexer.nextToken != Token.MULT_OP && Lexer.nextToken != Token.DIV_OP){
			Assign.resultOfTerm = Assign.resultOfFactor;
		}
	}
}

class Factor {  
	Expr e;
	int i;

	public Factor() {
		switch (Lexer.nextToken) {
		case Token.INT_LIT: // number
			Assign.resultOfFactor = Lexer.intValue;
			Lexer.lex();
			if (Lexer.intValue >=0 && Lexer.intValue < 6){
				Code.out.add("iconst_"+ Assign.resultOfFactor);
				Code.bytePosition += Code.getCost("iconst");
			} else if (Lexer.intValue >=6 && Lexer.intValue < 128) {
				Code.out.add("bipush "+ Assign.resultOfFactor);
				Code.bytePosition += Code.getCost("bipush");
			} else if (Lexer.intValue >= 128) {
				Code.out.add("sipush "+ Assign.resultOfFactor);
				Code.bytePosition += Code.getCost("sipush");
			}
			
			break;
		case Token.ID:
			Assign.resultOfFactor = Parser.id_value_map.get(Lexer.ident);
			Code.out.add("iload_" + Parser.id_map.get(Lexer.ident));
			Code.bytePosition += Code.getCost("iload");
			Lexer.lex();
			break;
		case Token.LEFT_PAREN: // '('
			Lexer.lex();
			e = new Expr();
			Assign.resultOfFactor = Assign.resultOfExpr;
			Lexer.lex(); // skip over ')'
			break;
		default:
			break;
		}
	}	 
}

class Code {
	static HashMap<Character, String> opcode= new HashMap<Character, String>();
	static HashMap<String, Integer> bytesToAddCode = new HashMap<String, Integer>();
	static int bytePosition = 0;
	public static void setCodes(){
		opcode.put('*', "imul");
		opcode.put('-', "isub");
		opcode.put('+', "iadd");
		opcode.put('/', "idiv");
	}
	
	public static void bytesToAdd(){
		bytesToAddCode.put("iconst", 1);
		bytesToAddCode.put("istore", 1);
		bytesToAddCode.put("bipush", 2);
		bytesToAddCode.put("sipush", 3);
		bytesToAddCode.put("imul", 1);
		bytesToAddCode.put("isub", 1);
		bytesToAddCode.put("iadd", 1);
		bytesToAddCode.put("idiv", 1);
		bytesToAddCode.put("return", 1);
		bytesToAddCode.put("iload", 1);
		bytesToAddCode.put("if_icmple", 3);
		bytesToAddCode.put("goto", 3);
		bytesToAddCode.put("if_icmpge", 3);
		bytesToAddCode.put("if_icmpeq", 3);
		bytesToAddCode.put("if_icmpne", 3);
		
	}
	
	public static String getCode(char a){
		return opcode.get(a);
	}

	static ArrayList<String> out = new ArrayList<String>();
	public static int getCost(String a){
		return Code.bytesToAddCode.get(a);
	}
	
	
	public static void output(){
		int bytecount = 0;
		Pattern p = Pattern.compile("(([a-z]+)_?([a-z]+))");
		Matcher m1;
		int bytes;
		for (String a:out){
			m1 = p.matcher(a);
			if (m1.find() == true)
			{
				bytes = Code.bytesToAddCode.get(m1.group());
			}
			else{
				bytes = 1;
			}
			System.out.println(bytecount + ": " + a);
			bytecount +=bytes;
		}
	out = new ArrayList<String>();
	}
}


