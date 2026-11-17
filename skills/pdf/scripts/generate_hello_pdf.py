from reportlab.lib.pagesizes import letter
from reportlab.pdfgen import canvas

def create_hello_pdf(output_path):
    # Create canvas
    c = canvas.Canvas(output_path, pagesize=letter)
    width, height = letter
    
    # Add title
    c.setFont("Helvetica-Bold", 24)
    c.drawString(100, height - 100, "Hello Skill")
    
    # Add description text
    c.setFont("Helvetica", 12)
    c.drawString(100, height - 130, "This is a PDF file generated using the pdf skill")
    c.drawString(100, height - 150, "with reportlab library.")
    c.drawString(100, height - 170, "The file demonstrates basic PDF creation capabilities.")
    
    # Save the PDF
    c.save()

if __name__ == "__main__":
    create_hello_pdf("output/hello-skill.pdf")
