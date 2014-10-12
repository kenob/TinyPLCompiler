import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Queue;

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

 	Sample Program - simple arithmetic with no paranthesis
int a, b, c , d;

a = 4; b = 5; c = 6;

d = 4 * 6 + 3;

end

 */

public class Parser {
	static HashMap<Character, Integer> id_map = new HashMap<Character, Integer>();
	static HashMap<Character, Integer> value_id_map = new HashMap<Character, Integer>();
	static int id = 0;
	public static void main(String[] args)  {
		System.out.println("Enter program and terminate with 'end'!\n");
		Code.setCodes();
		Lexer.lex();
		Program p = new Program();
		System.out.println("\n ----> Here is the output bytecode");
		Code.output();
		for(Integer abc: value_id_map.values()) System.out.println(abc);
		System.out.println("program ended!");
	}
}

class Program {
	public Program(){
		 
		Decls d = new Decls();
		Stmts s = new Stmts();	
		Code.out.add("return");
		 
	}	 
}

class Decls {
	public Decls(){
		if(Lexer.nextToken == Token.KEY_INT){
			Lexer.lex();	
		}
		Idlist id = new Idlist();
		
	}
}

class Idlist {
	 public Idlist(){
		 while (Lexer.nextToken != Token.SEMICOLON)
		 {
			 if (Lexer.nextToken == Token.ID){
				 Parser.id_map.put(Lexer.ident, Parser.id++);
			 }
			 Lexer.lex();
		 }
		 Lexer.lex();
	 }
}

class Stmt {
	Loop loop;
	Cond cond;
	Assign as;
	static char c;
	static int resultOfFactor;
	static int resultOfTerm = 1;
	static int resultOfExpr = 0;
	static Queue<String> loopQueue = new LinkedList<String>();
	
	
	public Stmt() {
		if (Lexer.nextToken == Token.KEY_WHILE) {
			loop = new Loop();
			solveLoop();
		} else if (Lexer.nextToken == Token.KEY_IF) {
			cond = new Cond();
			solveCond();
		} else if (Lexer.nextToken == Token.ID){
			c = Lexer.ident;
			as = new Assign();
			solveAssign();
		} else {
			return;
		}
	}
	
	public void solveLoop(){
		
	}
	
	public void solveCond(){
		Lexer.lex(); // if and '(' traversed uptil now
		cond.solveCondition();
		
	}
	
	public void solveAssign(){
		Lexer.lex();
		as.solveAssignment();
		return;
		
	}
}

class Stmts {
	 public Stmts()
	 {
		 new Stmt();
		 if (Lexer.nextToken == Token.SEMICOLON){
		 			 Lexer.lex();
		 			 if (Lexer.nextToken == Token.KEY_END){
		 				 return;
		 			 }
		 			 else{
		 				new Stmts();
		 			 }
		 			 
			 }
	 
	 }
			 
 }


class Assign {
	Expr ex;
	static Boolean firstTermCovered = false;
	static Boolean firstFactorCovered = false;
	
	public Assign(){
		ex = new Expr();
	}
	public void solveAssignment(){
		if (Lexer.nextToken == Token.ASSIGN_OP)
		{
			Lexer.lex();
		}
		ex.solveExpression();
		
		
	}
	 
}

class Cond {
	Rexpr re = new Rexpr();
	public void solveCondition(){
		re.solveRexpr();
		Lexer.lex(); // moves to next statements after if(a>b) {} 
	}
	 
}

class Loop {
	 
}

class Cmpdstmt {
	 
	
	public void solveCompound(){
		Lexer.lex(); 
		new Stmts();
	}
}

class Rexpr {
	Expr e1 = new Expr();
	Expr e2 = new Expr();
	int op;
	int valueExpr1;
	int valueExpr2;
	Cmpdstmt cmp = new Cmpdstmt();
	 public void solveRexpr(){
		 
		 // solving expression 1
		 Lexer.lex();
		 e1.solveExpression();
		 valueExpr1 = Stmt.resultOfExpr;
		 
		 // storing the comparison operator
		 op = Lexer.nextToken;
		 
		 // solving expression 2
		 Lexer.lex();
		 e2.solveExpression();
		 valueExpr2 = Stmt.resultOfExpr;
		 
		 switch (op){
		 	case 2:
		 		Code.out.add("if_icmpeq ");
		 		Lexer.lex(); // moves to compound Statement
		 		cmp.solveCompound();
		 		break;
		 		
		 	case 7:  // equal to
		 		Code.out.add("if_icmpne ");
		 		Lexer.lex(); // moves to compound Statement
		 		cmp.solveCompound();
		 		break;
		 		
		 	case 8:	 // greater than
		 		Code.out.add("if_icmple ");
		 		Lexer.lex(); // moves to compound Statement
		 		cmp.solveCompound();
		 		break;
		 		
		 	case 9:	 // lesser than
		 		Code.out.add("if_icmpge ");
		 		Lexer.lex(); // moves to compound Statement
		 		cmp.solveCompound();
		 		break;
		 
		 }
		 
	 }
}

class Expr {  
	Term t = new Term();
	public void solveExpression(){
		t.solveTerm();
		
		if (Assign.firstTermCovered == false) {
			Stmt.resultOfExpr = Stmt.resultOfTerm;
			Assign.firstTermCovered = true;
			//System.out.println(Stmt.resultOfExpr);
		}

		
		if (Lexer.nextToken == Token.ADD_OP) {
			
			int termresult = Stmt.resultOfTerm;
			
			Lexer.lex();
			new Expr().solveExpression();

			Stmt.resultOfExpr = Stmt.resultOfExpr + termresult;
			System.out.println(Stmt.resultOfExpr);
			return;
			
		} else if (Lexer.nextToken == Token.SUB_OP) {
			
			int termresult = Stmt.resultOfTerm;
			
			Lexer.lex();
			new Expr().solveExpression();

			Stmt.resultOfExpr = Stmt.resultOfExpr - termresult;
			return;
		} else if (Lexer.nextToken != Token.ADD_OP && Lexer.nextToken != Token.SUB_OP){
			Stmt.resultOfExpr = Stmt.resultOfTerm;
		}

		
		
	}
	 
}

class Term {  
	 Factor f = new Factor();
	 public void solveTerm(){
		f.solveFactor();
		 
		if (Assign.firstFactorCovered == false) {
			Stmt.resultOfTerm = Stmt.resultOfFactor;
			Assign.firstFactorCovered = true;
		}
		
		

		if (Lexer.nextToken == Token.MULT_OP) {
			int factresult = Stmt.resultOfFactor;
			Lexer.lex();
			new Term().solveTerm();

			Stmt.resultOfTerm = Stmt.resultOfTerm * factresult;
		} else if (Lexer.nextToken == Token.DIV_OP) {
			
			int factresult = Stmt.resultOfFactor;
			Lexer.lex();
			new Term().solveTerm();

			Stmt.resultOfTerm = Stmt.resultOfTerm / factresult;
		} else if (Lexer.nextToken != Token.MULT_OP && Lexer.nextToken != Token.DIV_OP){
			Stmt.resultOfTerm = Stmt.resultOfFactor;
		}
			
		 
	 }
}

class Factor { 
	
	public void solveFactor(){
		
		if(Lexer.nextToken == Token.ID){
			Stmt.resultOfFactor = Parser.value_id_map.get(Lexer.ident);
			Lexer.lex();
			return;
		} else if (Lexer.nextToken == Token.INT_LIT) {
			Stmt.resultOfFactor = Lexer.intValue;
			Lexer.lex();
			return; 
		} else if (Lexer.nextToken == Token.SEMICOLON) {
			return;
		}
		
	}
	 
}

class Code {
	static HashMap<Integer, String> opcode = new HashMap<Integer, String>();
	public static void setCodes(){
		opcode.put(Token.MULT_OP, "imult");
		opcode.put(Token.SUB_OP, "isub");
		opcode.put(Token.ADD_OP, "iadd");
		opcode.put(Token.DIV_OP, "idiv");
	}
	
	public static String getCode(int a){
		return opcode.get(a);
	}

	static ArrayList<String> out = new ArrayList<String>();
	public static void output(){
		for(int o = 0; o<out.size(); o++){
			System.out.println(o + " : " + out.get(o));
		}	
		out = new ArrayList<String>();
	}	 
	 
}


